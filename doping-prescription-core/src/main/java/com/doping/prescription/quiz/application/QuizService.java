package com.doping.prescription.quiz.application;

import com.doping.prescription.content.api.ContentCatalog;
import com.doping.prescription.content.api.MicroQuestionCatalog;
import com.doping.prescription.content.api.MicroQuestionCatalog.Option;
import com.doping.prescription.content.api.MicroQuestionCatalog.QuizQuestion;
import com.doping.prescription.content.api.TopicCatalog;
import com.doping.prescription.feed.api.ImpressionQuery;
import com.doping.prescription.feed.api.ImpressionQuery.ImpressionView;
import com.doping.prescription.points.api.PointsLedger;
import com.doping.prescription.quiz.application.QuizAnswering.AlreadyAnswered;
import com.doping.prescription.quiz.application.QuizAnswering.Answer;
import com.doping.prescription.quiz.application.QuizAnswering.Outcome;
import com.doping.prescription.quiz.application.QuizTokens.QuizClaims;
import com.doping.prescription.quiz.application.QuizViews.MasteryChangeView;
import com.doping.prescription.quiz.application.QuizViews.MicroQuiz;
import com.doping.prescription.quiz.application.QuizViews.OptionView;
import com.doping.prescription.quiz.application.QuizViews.QuizResult;
import com.doping.prescription.quiz.application.QuizViews.StreakView;
import com.doping.prescription.quiz.domain.OptionShuffle;
import com.doping.prescription.quiz.domain.OptionShuffle.Shuffled;
import com.doping.prescription.quiz.persistence.MicroQuizAttemptRepository;
import com.doping.prescription.shared.error.NotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/** Opens and grades micro-questions (docs/api.md, Akış → quiz endpoints; docs/algoritma.md §7). */
@Service
public class QuizService {

    private static final Duration RATE_WINDOW = Duration.ofMinutes(1);
    private static final int ANSWER_ATTEMPTS = 2;
    private static final Logger log = LoggerFactory.getLogger(QuizService.class);

    private final ImpressionQuery impressions;
    private final ContentCatalog content;
    private final MicroQuestionCatalog questions;
    private final TopicCatalog topics;
    private final PointsLedger points;
    private final MicroQuizAttemptRepository attempts;
    private final QuizTokens tokens;
    private final QuizTokenRegistry usedTokens;
    private final QuizAnswering answering;
    private final QuizProperties properties;
    private final Clock clock;
    private final RandomGenerator random;

    @SuppressWarnings("checkstyle:ParameterNumber") // one collaborator per module and per step of the answer
    QuizService(
            ImpressionQuery impressions,
            ContentCatalog content,
            MicroQuestionCatalog questions,
            TopicCatalog topics,
            PointsLedger points,
            MicroQuizAttemptRepository attempts,
            QuizTokens tokens,
            QuizTokenRegistry usedTokens,
            QuizAnswering answering,
            QuizProperties properties,
            Clock clock,
            RandomGenerator random) {
        this.impressions = impressions;
        this.content = content;
        this.questions = questions;
        this.topics = topics;
        this.points = points;
        this.attempts = attempts;
        this.tokens = tokens;
        this.usedTokens = usedTokens;
        this.answering = answering;
        this.properties = properties;
        this.clock = clock;
        this.random = random;
    }

    /**
     * The card's next unanswered question with shuffled options, or empty if there is none (204).
     *
     * @throws QuizErrors.VideoNotWatched if less than {@code min-watch-ratio} of the video was watched
     */
    public Optional<MicroQuiz> open(UUID userId, UUID impressionId) {
        ImpressionView impression =
                ownImpression(userId, impressionId).orElseThrow(() -> new NotFoundException("Kart bulunamadı."));
        long durationMs = Duration.ofSeconds(content.video(impression.contentId())
                        .orElseThrow(() -> new NotFoundException("Video bulunamadı."))
                        .durationSec())
                .toMillis();
        if (impression.maxWatchMs() < properties.minWatchRatio() * durationMs) {
            throw new QuizErrors.VideoNotWatched();
        }
        return questions
                .nextQuestion(impression.contentId(), new HashSet<>(attempts.findAnsweredQuestionIds(userId)))
                .map(question -> quiz(userId, impression, question));
    }

    /** Grades an answer; a second answer to the same question earns 0 points (docs/api.md). */
    public QuizResult answer(
            UUID userId, Integer microQuestionId, String quizToken, String selectedKey, int responseMs) {
        if (attempts.countSince(userId, clock.instant().minus(RATE_WINDOW)) >= properties.rateLimitPerMin()) {
            throw new QuizErrors.RateLimited();
        }
        QuizClaims claims = tokens.verify(quizToken);
        if (!claims.userId().equals(userId) || !claims.microQuestionId().equals(microQuestionId)) {
            throw new QuizErrors.TokenInvalid();
        }
        String storedKey = claims.shownToStored().get(selectedKey);
        if (storedKey == null) {
            throw new QuizErrors.UnknownOption();
        }
        QuizQuestion question = questions.question(microQuestionId).orElseThrow(QuizErrors.TokenInvalid::new);
        ImpressionView impression =
                ownImpression(userId, claims.impressionId()).orElseThrow(QuizErrors.TokenInvalid::new);

        boolean correct = storedKey.equals(question.correctKey());
        Outcome outcome = grade(
                claims,
                new Answer(
                        userId,
                        microQuestionId,
                        impression.impressionId(),
                        question.topicId(),
                        storedKey,
                        correct,
                        responseMs,
                        impression.weakTopic()));
        return result(correct, claims, question, outcome);
    }

    private QuizResult result(boolean correct, QuizClaims claims, QuizQuestion question, Outcome outcome) {
        return new QuizResult(
                correct,
                Shuffled.shownKeyOf(claims.shownToStored(), question.correctKey()),
                question.explanation(),
                outcome.award().pointsAwarded(),
                outcome.award().totalPoints(),
                new StreakView(outcome.award().streak(), outcome.award().bonusAt()),
                new MasteryChangeView(
                        question.topicId(),
                        topics.topicsById().get(question.topicId()).name(),
                        outcome.change().before(),
                        outcome.change().after()));
    }

    /**
     * The answer counts only on the token's first use and the question's first answer; otherwise it earns nothing. An
     * answer that fails before it is stored gives the token back, so the learner can send it again.
     */
    private Outcome grade(QuizClaims claims, Answer answer) {
        if (!usedTokens.firstUse(claims.tokenId(), properties.tokenTtl())) {
            return answering.unchanged(answer.userId(), answer.topicId());
        }
        boolean settled = false;
        try {
            Outcome outcome = answerRetryingConflicts(answer);
            settled = true;
            return outcome;
        } catch (AlreadyAnswered alreadyAnswered) {
            settled = true;
            return answering.unchanged(answer.userId(), answer.topicId());
        } finally {
            if (!settled) {
                usedTokens.release(claims.tokenId());
            }
        }
    }

    /**
     * A concurrent answer of the same learner can create the same mastery or streak row first (unique key) or change it
     * (version); the transaction rolls back and one retry reads the committed row.
     */
    private Outcome answerRetryingConflicts(Answer answer) {
        for (int attempt = 1; ; attempt++) {
            try {
                return answering.answerOnce(answer);
            } catch (ConcurrencyFailureException | DataIntegrityViolationException conflict) {
                if (attempt >= ANSWER_ATTEMPTS) {
                    throw conflict;
                }
                log.warn("Retrying a quiz answer after a concurrent update userId={}", answer.userId());
            }
        }
    }

    private MicroQuiz quiz(UUID userId, ImpressionView impression, QuizQuestion question) {
        Map<String, String> stored = new LinkedHashMap<>();
        for (Option option : question.options()) {
            stored.put(option.key(), option.text());
        }
        Shuffled shuffled = OptionShuffle.shuffle(stored, random);
        String token = tokens.issue(new QuizClaims(
                UUID.randomUUID(),
                userId,
                impression.impressionId(),
                question.microQuestionId(),
                shuffled.shownToStored()));
        return new MicroQuiz(
                question.microQuestionId(),
                impression.impressionId(),
                question.stem(),
                shuffled.shown().entrySet().stream()
                        .map(entry -> new OptionView(entry.getKey(), entry.getValue()))
                        .toList(),
                properties.timeLimitSec(),
                points.pointsAvailable(impression.weakTopic()),
                token);
    }

    private Optional<ImpressionView> ownImpression(UUID userId, UUID impressionId) {
        return impressions
                .impression(impressionId)
                .filter(impression -> impression.userId().equals(userId));
    }
}
