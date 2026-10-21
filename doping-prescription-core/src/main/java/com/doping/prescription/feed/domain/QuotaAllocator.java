package com.doping.prescription.feed.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Splits a page between pools (docs/algoritma.md §2): largest remainder method, ties in pool order, and a pool's
 * shortfall moves along WEAK_TOPIC → EXPLORE → POPULAR.
 */
public final class QuotaAllocator {

    private static final List<PoolType> POOL_ORDER =
            List.of(PoolType.WEAK_TOPIC, PoolType.REVIEW_DUE, PoolType.EXPLORE, PoolType.POPULAR);
    private static final List<PoolType> SHORTFALL_CHAIN =
            List.of(PoolType.WEAK_TOPIC, PoolType.EXPLORE, PoolType.POPULAR);

    private QuotaAllocator() {}

    /**
     * @param quotas    share of each pool, summing to 1; pools without a quota get nothing
     * @param size      cards on the page
     * @param available candidates each pool can offer
     * @return cards per pool; the total is below {@code size} only if all pools together run out
     */
    public static Map<PoolType, Integer> allocate(
            Map<PoolType, Double> quotas, int size, Map<PoolType, Integer> available) {
        Map<PoolType, Integer> counts = new EnumMap<>(PoolType.class);
        List<PoolType> pools = POOL_ORDER.stream().filter(quotas::containsKey).toList();
        int assigned = 0;
        for (PoolType pool : pools) {
            int base = (int) Math.floor(quotas.get(pool) * size);
            counts.put(pool, base);
            assigned += base;
        }
        List<PoolType> byRemainder = new ArrayList<>(pools);
        byRemainder.sort(Comparator.comparingDouble((PoolType pool) -> remainder(quotas.get(pool), size))
                .reversed()
                .thenComparing(POOL_ORDER::indexOf));
        for (int index = 0; assigned < size; index++, assigned++) {
            PoolType pool = byRemainder.get(index % byRemainder.size());
            counts.merge(pool, 1, Integer::sum);
        }

        for (PoolType pool : pools) {
            int shortfall = counts.get(pool) - available.getOrDefault(pool, 0);
            if (shortfall <= 0) {
                continue;
            }
            counts.put(pool, counts.get(pool) - shortfall);
            for (PoolType receiver : SHORTFALL_CHAIN) {
                int spare = available.getOrDefault(receiver, 0) - counts.getOrDefault(receiver, 0);
                int moved = Math.min(spare, shortfall);
                if (receiver != pool && moved > 0) {
                    counts.merge(receiver, moved, Integer::sum);
                    shortfall -= moved;
                }
            }
        }
        return counts;
    }

    private static double remainder(double quota, int size) {
        double exact = quota * size;
        return exact - Math.floor(exact);
    }
}
