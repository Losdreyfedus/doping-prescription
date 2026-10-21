package com.doping.prescription.admin.application;

import com.doping.prescription.admin.domain.DemoPersona;
import com.doping.prescription.auth.api.UserDirectory;
import com.doping.prescription.auth.api.UserDirectory.UserSummary;
import com.doping.prescription.content.api.TopicCatalog;
import com.doping.prescription.content.api.TopicCatalog.TopicView;
import com.doping.prescription.exam.api.ExamSubmission;
import com.doping.prescription.feed.api.ImpressionQuery;
import com.doping.prescription.feed.api.ImpressionQuery.DebugCard;
import com.doping.prescription.learner.api.LearnerProfileQuery;
import com.doping.prescription.learner.api.LearnerProfileQuery.TopicProfile;
import com.doping.prescription.points.api.PointsLedger;
import com.doping.prescription.shared.api.UserDataEraser;
import com.doping.prescription.shared.error.DomainException;
import com.doping.prescription.shared.error.ErrorCode;
import com.doping.prescription.shared.error.NotFoundException;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The admin's demo control (docs/api.md, Admin): reset a student, load a persona, the presenter's answer key and the
 * "why this feed" debug view. Writes go only through other modules' public APIs; admin owns no tables.
 */
@Service
public class DemoControlService {

    private static final Logger log = LoggerFactory.getLogger(DemoControlService.class);
    private static final String STUDENT = "STUDENT";
    private static final int WEAKEST_TOPICS = 5;

    private final UserDirectory users;
    private final List<UserDataEraser> erasers;
    private final Map<String, DemoPersona> personas;
    private final ExamSubmission exams;
    private final LearnerProfileQuery profiles;
    private final ImpressionQuery impressions;
    private final PointsLedger points;
    private final TopicCatalog topics;

    @SuppressWarnings("checkstyle:ParameterNumber") // the demo control reads from every module
    DemoControlService(
            UserDirectory users,
            List<UserDataEraser> erasers,
            List<DemoPersona> personas,
            ExamSubmission exams,
            LearnerProfileQuery profiles,
            ImpressionQuery impressions,
            PointsLedger points,
            TopicCatalog topics) {
        this.users = users;
        this.erasers = List.copyOf(erasers);
        this.personas = personas.stream().collect(Collectors.toMap(DemoPersona::name, Function.identity()));
        this.exams = exams;
        this.profiles = profiles;
        this.impressions = impressions;
        this.points = points;
        this.topics = topics;
    }

    /** The demo students with what the presenter needs to see before going on stage. */
    public List<StudentCard> students() {
        return users.allUsers().stream()
                .filter(user -> STUDENT.equals(user.role()))
                .map(user -> new StudentCard(
                        user.slug(),
                        user.displayName(),
                        user.personalizationEnabled(),
                        points.summary(user.userId()).total(),
                        profiles.profile(user.userId()).lastExamAttemptId().isPresent()))
                .toList();
    }

    /** Erases the student's attempts, signals, mastery, feed history, quiz answers and points (and Redis keys). */
    public void reset(String slug) {
        UUID userId = student(slug).userId();
        erasers.forEach(eraser -> eraser.erase(userId));
        log.info(
                "Demo reset slug={} modules={}",
                slug,
                erasers.stream().map(UserDataEraser::name).toList());
    }

    /** Resets the student, then brings them into the persona's state. */
    public void loadPersona(String slug, String personaName) {
        DemoPersona persona = personas.get(personaName);
        if (persona == null) {
            throw new UnknownPersonaException(personas.keySet());
        }
        reset(slug);
        persona.apply(student(slug).userId());
        log.info("Demo persona loaded slug={} persona={}", slug, personaName);
    }

    public List<String> personaNames() {
        return personas.keySet().stream().sorted().toList();
    }

    /** Mini Deneme 1's correct and scenario answers for the presenter. */
    public List<AnswerKeyRow> answerKey() {
        Map<Integer, TopicView> catalog = topics.topicsById();
        return exams.answerKey(ScriptedInProgressPersona.MINI_EXAM_1).stream()
                .map(row -> new AnswerKeyRow(
                        row.orderNo(),
                        catalog.get(row.topicId()).name(),
                        row.correctKey(),
                        row.scenarioKey().orElse(null)))
                .toList();
    }

    /** The student's weakest topics and the cards of their latest feed session with the score components. */
    public FeedDebug feedDebug(String slug) {
        UUID userId = student(slug).userId();
        Map<Integer, TopicView> catalog = topics.topicsById();
        List<WeakTopic> weakest = profiles.profile(userId).topics().stream()
                .filter(topic -> topic.attempts() > 0)
                .sorted(Comparator.comparingDouble(TopicProfile::effectiveMastery))
                .limit(WEAKEST_TOPICS)
                .map(topic -> new WeakTopic(
                        topic.topicId(),
                        catalog.get(topic.topicId()).name(),
                        topic.effectiveMastery(),
                        topic.confidence(),
                        topic.band()))
                .toList();
        return new FeedDebug(slug, weakest, impressions.lastSession(userId));
    }

    private UserSummary student(String slug) {
        return users.findBySlug(slug)
                .filter(user -> STUDENT.equals(user.role()))
                .orElseThrow(() -> new NotFoundException("Öğrenci bulunamadı."));
    }

    /**
     * @param hasExam whether the student has a processed exam result
     */
    public record StudentCard(
            String slug, String displayName, boolean personalizationEnabled, int totalPoints, boolean hasExam) {}

    /**
     * @param scenarioKey the answer given on stage; {@code null} means left blank
     */
    public record AnswerKeyRow(
            int orderNo,
            String topic,
            String correctKey,
            @Schema(nullable = true) @Nullable String scenarioKey) {}

    public record WeakTopic(Integer topicId, String name, double mastery, double confidence, String band) {}

    /**
     * @param weakestTopics up to 5 topics with data, weakest first
     * @param cards         the latest feed session in position order
     */
    public record FeedDebug(String slug, List<WeakTopic> weakestTopics, List<DebugCard> cards) {

        public FeedDebug {
            weakestTopics = List.copyOf(weakestTopics);
            cards = List.copyOf(cards);
        }
    }

    /** docs/mimari.md §10: an unknown persona name answers 400. */
    static final class UnknownPersonaException extends DomainException {

        private static final long serialVersionUID = 1L;

        UnknownPersonaException(Collection<String> known) {
            super(ErrorCode.INVALID_REQUEST, "Bilinmeyen persona. Seçenekler: " + String.join(", ", known));
        }
    }
}
