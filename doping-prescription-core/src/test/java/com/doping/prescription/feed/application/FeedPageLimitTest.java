package com.doping.prescription.feed.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.doping.prescription.feed.application.FeedService.InvalidLimitException;
import com.doping.prescription.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** The page size of GET /api/feed comes from prescription.feed.page-size / page-size-max, not from the controller. */
class FeedPageLimitTest {

    /** Deliberately not the defaults (10 / 20), so a hard-coded value cannot pass. */
    private static final FeedProperties PROPERTIES = TestFeedProperties.withPageSizes(7, 12);

    @Test
    void withoutALimitThePageHasTheConfiguredSize() {
        assertThat(FeedService.pageLimit(null, PROPERTIES)).isEqualTo(7);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 7, 12})
    void aLimitWithinOneToTheConfiguredMaximumIsKept(int limit) {
        assertThat(FeedService.pageLimit(limit, PROPERTIES)).isEqualTo(limit);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 13})
    void aLimitOutsideOneToTheConfiguredMaximumIsAnInvalidRequest(int limit) {
        assertThatThrownBy(() -> FeedService.pageLimit(limit, PROPERTIES))
                .isInstanceOfSatisfying(
                        InvalidLimitException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_REQUEST))
                .hasMessageContaining("12");
    }
}
