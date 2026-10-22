package com.doping.prescription.admin.application;

import com.doping.prescription.content.api.ContentCatalog;
import com.doping.prescription.content.api.ContentCatalog.VideoView;
import com.doping.prescription.content.api.MicroQuestionCatalog;
import com.doping.prescription.content.api.TopicCatalog;
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
        List<QuestionStats> stats = quizzes.answersByQuestion();
        Map<Integer, Integer> videoOf = questions.contentIdsOf(
                stats.stream().map(QuestionStats::microQuestionId).toList());
        Map<Integer, long[]> totals = new HashMap<>();
        for (QuestionStats question : stats) {
            Integer contentId = videoOf.get(question.microQuestionId());
            if (contentId != null) {
                long[] total = totals.computeIfAbsent(contentId, id -> new long[2]);
                total[0] += question.answers();
                total[1] += question.correct();
            }
        }
        Map<Integer, VideoView> videos = content.videos(totals.keySet()).stream()
                .collect(Collectors.toMap(VideoView::contentId, Function.identity()));
        Map<Integer, TopicCatalog.TopicView> catalog = topics.topicsById();
        return totals.entrySet().stream()
                .filter(entry -> videos.containsKey(entry.getKey()))
                .map(entry -> {
                    VideoView video = videos.get(entry.getKey());
                    long answers = entry.getValue()[0];
                    long correct = entry.getValue()[1];
                    return new VideoEffectiveness(
                            video.contentId(),
                            video.title(),
                            catalog.get(video.primaryTopic()).name(),
                            answers,
                            correct,
                            (double) correct / answers);
                })
                .sorted(ORDER)
                .toList();
    }

    /**
     * @param correctRate correct answers / answers, 0-1
     */
    public record VideoEffectiveness(
            Integer contentId, String title, String topic, long answers, long correct, double correctRate) {}
}
