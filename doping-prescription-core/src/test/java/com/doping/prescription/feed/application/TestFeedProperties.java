package com.doping.prescription.feed.application;

import java.time.Duration;
import java.util.List;

/** {@link FeedProperties} with the values of application.yml, for unit tests that do not start Spring. */
public final class TestFeedProperties {

    public static final Duration STORE_RETRY_AFTER = Duration.ofSeconds(10);

    private TestFeedProperties() {}

    public static FeedProperties defaults() {
        return withPageSizes(10, 20);
    }

    public static FeedProperties withPageSizes(int pageSize, int pageSizeMax) {
        return new FeedProperties(
                pageSize,
                pageSizeMax,
                30,
                Duration.ofMinutes(30),
                5,
                Duration.ofMinutes(30),
                STORE_RETRY_AFTER,
                new FeedProperties.Quota(0.6, 0.15, 0.15, 0.1),
                new FeedProperties.ColdStartQuota(0.7, 0.3),
                5,
                7,
                new FeedProperties.Weights(0.45, 0.2, 0.15, 0.1, 0.1),
                60,
                0.3,
                2,
                14,
                3,
                4,
                List.of());
    }
}
