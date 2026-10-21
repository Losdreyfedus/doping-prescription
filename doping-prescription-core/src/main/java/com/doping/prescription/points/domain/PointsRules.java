package com.doping.prescription.points.domain;

/**
 * The points rules of docs/algoritma.md §7: base points for a correct answer, a bonus at every completed streak, and a
 * daily cap that limits both. Pure.
 *
 * @param correct          a correct answer
 * @param weakTopicCorrect a correct answer on a WEAK_TOPIC card
 * @param streakLength     consecutive correct answers per bonus
 * @param streakBonus      the bonus
 * @param dailyCap         most points per calendar day
 */
public record PointsRules(int correct, int weakTopicCorrect, int streakLength, int streakBonus, int dailyCap) {

    public PointsRules {
        if (correct <= 0 || weakTopicCorrect <= 0 || streakLength <= 0 || streakBonus <= 0 || dailyCap <= 0) {
            throw new IllegalArgumentException("points rules must be positive");
        }
    }

    public int available(boolean weakTopic) {
        return weakTopic ? weakTopicCorrect : correct;
    }

    /**
     * @param streakAfter  the streak including this answer
     * @param earnedToday  points already earned today
     */
    public Award award(boolean wasCorrect, boolean weakTopic, int streakAfter, int earnedToday) {
        if (!wasCorrect) {
            return new Award(0, 0);
        }
        int room = Math.max(0, dailyCap - earnedToday);
        int base = Math.min(available(weakTopic), room);
        int bonus = streakAfter > 0 && streakAfter % streakLength == 0 ? Math.min(streakBonus, room - base) : 0;
        return new Award(base, bonus);
    }

    /** Points for the answer and for a completed streak, after the daily cap. */
    public record Award(int base, int bonus) {

        public int total() {
            return base + bonus;
        }
    }
}
