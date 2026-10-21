package com.doping.prescription.feed.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class QuotaAllocatorTest {

    private static final Map<PoolType, Integer> PLENTY =
            Map.of(PoolType.WEAK_TOPIC, 50, PoolType.REVIEW_DUE, 50, PoolType.EXPLORE, 50, PoolType.POPULAR, 50);

    @Test
    void aTenCardPageIsSplitSixTwoOneOne() {
        assertThat(QuotaAllocator.allocate(FeedFixtures.quotas(), 10, PLENTY))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        PoolType.WEAK_TOPIC, 6, PoolType.REVIEW_DUE, 2, PoolType.EXPLORE, 1, PoolType.POPULAR, 1));
    }

    @Test
    void aShortPoolHandsItsCardsAlongTheChain() {
        Map<PoolType, Integer> available =
                Map.of(PoolType.WEAK_TOPIC, 7, PoolType.REVIEW_DUE, 0, PoolType.EXPLORE, 50, PoolType.POPULAR, 50);

        assertThat(QuotaAllocator.allocate(FeedFixtures.quotas(), 10, available))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        PoolType.WEAK_TOPIC, 7, PoolType.REVIEW_DUE, 0, PoolType.EXPLORE, 2, PoolType.POPULAR, 1));
    }

    @Test
    void thePageIsShorterOnlyWhenEveryPoolRunsOut() {
        Map<PoolType, Integer> available =
                Map.of(PoolType.WEAK_TOPIC, 1, PoolType.REVIEW_DUE, 0, PoolType.EXPLORE, 1, PoolType.POPULAR, 2);

        assertThat(QuotaAllocator.allocate(FeedFixtures.quotas(), 10, available).values().stream()
                        .mapToInt(Integer::intValue)
                        .sum())
                .isEqualTo(4);
    }
}
