package com.doping.prescription.points.application;

import com.doping.prescription.points.api.PointsLedger;
import com.doping.prescription.points.domain.PointsReason;
import com.doping.prescription.points.domain.PointsRules;
import com.doping.prescription.points.persistence.PointsLedgerEntry;
import com.doping.prescription.points.persistence.PointsLedgerRepository;
import com.doping.prescription.points.persistence.PointsStreak;
import com.doping.prescription.points.persistence.PointsStreakRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies {@link PointsRules} to the ledger and the streak (docs/algoritma.md §7). */
@Service
class PointsLedgerService implements PointsLedger {

    private final PointsLedgerRepository ledger;
    private final PointsStreakRepository streaks;
    private final PointsRules rules;
    private final Clock clock;

    PointsLedgerService(
            PointsLedgerRepository ledger, PointsStreakRepository streaks, PointsProperties properties, Clock clock) {
        this.ledger = ledger;
        this.streaks = streaks;
        this.rules = properties.rules();
        this.clock = clock;
    }

    @Override
    public int pointsAvailable(boolean weakTopic) {
        return rules.available(weakTopic);
    }

    @Override
    @Transactional
    public PointsLedger.Award award(UUID userId, String refId, boolean correct, boolean weakTopic) {
        Instant now = clock.instant();
        String answerRef = "quiz:" + refId;
        PointsStreak streak = streaks.findById(userId).orElseGet(() -> new PointsStreak(userId, now));
        if (ledger.existsByRefId(answerRef)) {
            return new PointsLedger.Award(0, total(userId), streak.getCurrent(), rules.streakLength());
        }
        streak.apply(refId, correct, now);
        streaks.save(streak);

        PointsRules.Award award =
                rules.award(correct, weakTopic, streak.getCurrent(), ledger.sumSince(userId, startOfToday()));
        if (award.base() > 0) {
            PointsReason reason = weakTopic ? PointsReason.QUIZ_CORRECT_WEAK : PointsReason.QUIZ_CORRECT;
            ledger.save(new PointsLedgerEntry(userId, award.base(), reason, answerRef, now));
        }
        if (award.bonus() > 0) {
            ledger.save(
                    new PointsLedgerEntry(userId, award.bonus(), PointsReason.STREAK_BONUS, "streak:" + refId, now));
        }
        return new PointsLedger.Award(award.total(), total(userId), streak.getCurrent(), rules.streakLength());
    }

    @Override
    @Transactional(readOnly = true)
    public PointsLedger.Award current(UUID userId) {
        int streak = streaks.findById(userId).map(PointsStreak::getCurrent).orElse(0);
        return new PointsLedger.Award(0, total(userId), streak, rules.streakLength());
    }

    @Override
    @Transactional(readOnly = true)
    public Summary summary(UUID userId) {
        PointsStreak streak = streaks.findById(userId).orElseGet(() -> new PointsStreak(userId, clock.instant()));
        return new Summary(
                total(userId),
                ledger.sumSince(userId, startOfToday()),
                rules.dailyCap(),
                new Streak(streak.getCurrent(), streak.getBest()),
                ledger.findTop10ByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                        .map(entry ->
                                new Entry(entry.getAmount(), entry.getReason().name(), entry.getAwardedAt()))
                        .toList());
    }

    private int total(UUID userId) {
        return ledger.sumSince(userId, Instant.EPOCH);
    }

    /** "Today" in prescription.timezone, the clock's zone. */
    private Instant startOfToday() {
        return LocalDate.now(clock).atStartOfDay(clock.getZone()).toInstant();
    }
}
