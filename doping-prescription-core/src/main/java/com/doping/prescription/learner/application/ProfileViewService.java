package com.doping.prescription.learner.application;

import com.doping.prescription.auth.api.UserDirectory;
import com.doping.prescription.auth.api.UserDirectory.UserSummary;
import com.doping.prescription.content.api.ContentCatalog;
import com.doping.prescription.content.api.TopicCatalog;
import com.doping.prescription.content.api.TopicCatalog.TopicView;
import com.doping.prescription.learner.api.LearnerProfileQuery;
import com.doping.prescription.learner.api.LearnerProfileQuery.Band;
import com.doping.prescription.learner.api.LearnerProfileQuery.LearnerProfile;
import com.doping.prescription.learner.api.LearnerProfileQuery.TopicProfile;
import com.doping.prescription.learner.api.LearnerProfileQuery.Trend;
import com.doping.prescription.learner.domain.MasteryRounding;
import com.doping.prescription.shared.error.NotFoundException;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/** The Gelişimim view of a learner's profile (docs/api.md, GET /api/learner/profile). */
@Service
public class ProfileViewService {

    /** Weakest first; topics without data last (docs/api.md). */
    private static final Comparator<TopicProfileView> DISPLAY_ORDER = Comparator.comparing(
                    (TopicProfileView topic) -> topic.band() == Band.UNKNOWN)
            .thenComparingDouble(TopicProfileView::mastery)
            .thenComparing(TopicProfileView::topicId);

    private final LearnerProfileQuery profiles;
    private final TopicCatalog topics;
    private final ContentCatalog content;
    private final UserDirectory users;

    ProfileViewService(LearnerProfileQuery profiles, TopicCatalog topics, ContentCatalog content, UserDirectory users) {
        this.profiles = profiles;
        this.topics = topics;
        this.content = content;
        this.users = users;
    }

    public ProfileView profile(UUID userId) {
        UserSummary user = users.findById(userId).orElseThrow(() -> new NotFoundException("Kullanıcı bulunamadı."));
        LearnerProfile profile = profiles.profile(userId);
        Map<Integer, TopicView> topicsById = topics.topicsById();
        Map<Integer, Integer> readyContent = content.liveVideoCountByTopic();
        List<TopicProfileView> topicViews = profile.topics().stream()
                .map(topic -> view(topic, topicsById.get(topic.topicId()), readyContent))
                .sorted(DISPLAY_ORDER)
                .toList();
        return new ProfileView(
                userId,
                user.personalizationEnabled(),
                profile.lastExamAttemptId(),
                profile.topics().stream()
                        .map(TopicProfile::lastSignalAt)
                        .filter(Objects::nonNull)
                        .max(Comparator.naturalOrder())
                        .orElse(null),
                topicViews);
    }

    private static TopicProfileView view(TopicProfile topic, TopicView catalog, Map<Integer, Integer> readyContent) {
        return new TopicProfileView(
                topic.topicId(),
                catalog.code(),
                catalog.name(),
                MasteryRounding.forApi(topic.effectiveMastery()).doubleValue(),
                MasteryRounding.forApi(topic.confidence()).doubleValue(),
                topic.band(),
                topic.trend(),
                topic.lastSignalAt(),
                readyContent.getOrDefault(topic.topicId(), 0));
    }

    /**
     * @param lastExamAttemptId the latest exam attempt reflected in the profile; the result screen polls for it
     * @param updatedAt         time of the latest signal
     * @param topics            weakest first, topics without data last
     */
    public record ProfileView(
            UUID userId,
            boolean personalizationEnabled,
            @Schema(nullable = true) @Nullable UUID lastExamAttemptId,
            @Schema(nullable = true) @Nullable Instant updatedAt,
            List<TopicProfileView> topics) {

        public ProfileView {
            topics = List.copyOf(topics);
        }
    }

    /**
     * @param mastery           decayed mastery, 3 decimals (HALF_UP)
     * @param readyContentCount LIVE videos whose primary topic this is
     */
    public record TopicProfileView(
            Integer topicId,
            String code,
            String name,
            double mastery,
            double confidence,
            Band band,
            Trend trend,
            @Schema(nullable = true) @Nullable Instant lastSignalAt,
            int readyContentCount) {}
}
