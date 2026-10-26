package com.doping.prescription.admin.application;

import com.doping.prescription.content.api.ContentCatalog;
import com.doping.prescription.content.api.ContentCatalog.VideoView;
import com.doping.prescription.content.api.MicroQuestionCatalog;
import com.doping.prescription.content.api.TopicCatalog;
import com.doping.prescription.content.api.TopicCatalog.TopicView;
import com.doping.prescription.quiz.api.QuizStatistics;
import com.doping.prescription.quiz.api.QuizStatistics.QuestionStats;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * How well each video prepares learners for its micro-questions: answers and the share of correct ones, from the quiz
 * module's answers mapped to videos through the content module (docs/api.md, GET /api/admin/content-effectiveness).
 */
@Service
public class ContentEffectivenessService {

    /** Shown for a video whose primary topic is not in the catalog; the row still counts, only its label is unknown. */
    static final String UNKNOWN_TOPIC = "?";

    /** Best first; among equal rates, the one with more evidence first. */
    private static final Comparator<VideoEffectiveness> ORDER = Comparator.comparingDouble(
                    VideoEffectiveness::correctRate)
            .reversed()
            .thenComparing(Comparator.comparingLong(VideoEffectiveness::answers).reversed())
            .thenComparing(VideoEffectiveness::contentId);

    private final QuizStatistics quizzes;
    private final MicroQuestionCatalog questions;
    private final ContentCatalog content;
    private final TopicCatalog topics;

    ContentEffectivenessService(
            QuizStatistics quizzes, MicroQuestionCatalog questions, ContentCatalog content, TopicCatalog topics) {
        this.quizzes = quizzes;
        this.questions = questions;
        this.content = content;
        this.topics = topics;
    }

    public List<VideoEffectiveness> byVideo() {
        Map<Integer, Tally> tallies = talliesByVideo(quizzes.answersByQuestion());
        Map<Integer, VideoView> videos = content.videos(tallies.keySet()).stream()
                .collect(Collectors.toMap(VideoView::contentId, Function.identity()));
        Map<Integer, TopicView> catalog = topics.topicsById();
        return tallies.entrySet().stream()
                .filter(entry -> videos.containsKey(entry.getKey()))
                .map(entry -> effectiveness(videos.get(entry.getKey()), entry.getValue(), catalog))
                .sorted(ORDER)
                .toList();
    }

    /** Sums the questions' answers per video; questions the content module no longer knows are left out. */
    private Map<Integer, Tally> talliesByVideo(List<QuestionStats> stats) {
        Map<Integer, Integer> videoOf = questions.contentIdsOf(
                stats.stream().map(QuestionStats::microQuestionId).toList());
        Map<Integer, Tally> tallies = new HashMap<>();
        for (QuestionStats question : stats) {
            Integer contentId = videoOf.get(question.microQuestionId());
            if (contentId != null) {
                tallies.merge(contentId, new Tally(question.answers(), question.correct()), Tally::plus);
            }
        }
        return tallies;
    }

    private static VideoEffectiveness effectiveness(VideoView video, Tally tally, Map<Integer, TopicView> catalog) {
        TopicView topic = catalog.get(video.primaryTopic());
        return new VideoEffectiveness(
                video.contentId(),
                video.title(),
                topic == null ? UNKNOWN_TOPIC : topic.name(),
                tally.answers(),
                tally.correct(),
                tally.correctRate());
    }

    /** Answers and correct answers of one video's questions. */
    record Tally(long answers, long correct) {

        Tally plus(Tally other) {
            return new Tally(answers + other.answers, correct + other.correct);
        }

        /** 0 without answers, so a video nobody answered yet never divides by zero. */
        double correctRate() {
            return answers == 0 ? 0 : (double) correct / answers;
        }
    }

    /**
     * @param correctRate correct answers / answers, 0-1
     */
    public record VideoEffectiveness(
            Integer contentId, String title, String topic, long answers, long correct, double correctRate) {}
}
