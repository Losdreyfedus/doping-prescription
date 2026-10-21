package com.doping.prescription.feed.domain;

import com.doping.prescription.feed.domain.FeedContext.TopicState;
import java.time.Duration;
import java.util.List;

/**
 * The "neden bu video" templates of docs/algoritma.md §6, one per pool. Suffixes are never attached to topic names, so
 * no Turkish vowel harmony is needed.
 */
public final class ReasonTemplates {

    private ReasonTemplates() {}

    public static List<ReasonTemplate> all() {
        return List.of(weakTopic(), reviewDue(), explore(), popular(), fallback());
    }

    public static ReasonTemplate weakTopic() {
        return of(PoolType.WEAK_TOPIC, (candidate, context) -> {
            TopicState topic = context.topic(candidate.targetTopicId());
            return topic.lastExam()
                    .map(exam -> "Son denemende %s: %d soruda %d doğru"
                            .formatted(topic.name(), exam.total(), exam.correct()))
                    .orElseGet(() -> "%s sorularında zorlanıyorsun".formatted(topic.name()));
        });
    }

    public static ReasonTemplate reviewDue() {
        return of(PoolType.REVIEW_DUE, (candidate, context) -> {
            TopicState topic = context.topic(candidate.targetTopicId());
            long days = topic.lastSignalAt()
                    .map(last -> Duration.between(last, context.now()).toDays())
                    .orElse(0L);
            return "%s konusunu %d gündür tekrar etmedin".formatted(topic.name(), days);
        });
    }

    public static ReasonTemplate explore() {
        return of(
                PoolType.EXPLORE,
                (candidate, context) -> "TYT'de sık çıkan konu: "
                        + context.topic(candidate.targetTopicId()).name());
    }

    public static ReasonTemplate popular() {
        return of(PoolType.POPULAR, (candidate, context) -> "Bu hafta en çok izlenenlerden");
    }

    public static ReasonTemplate fallback() {
        return of(PoolType.FALLBACK, (candidate, context) -> "Dopingle'da yeni");
    }

    private interface Text {
        String of(Candidate candidate, FeedContext context);
    }

    private static ReasonTemplate of(PoolType pool, Text text) {
        return new ReasonTemplate() {
            @Override
            public PoolType pool() {
                return pool;
            }

            @Override
            public String reason(Candidate candidate, FeedContext context) {
                return text.of(candidate, context);
            }
        };
    }
}
