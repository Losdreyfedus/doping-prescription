package com.doping.prescription.quiz.application;

import java.time.Duration;
import java.util.UUID;

/** Remembers used quiz tokens (docs/mimari.md §5, quiz:token-used:{jti}). */
public interface QuizTokenRegistry {

    /**
     * Marks the token as used.
     *
     * @return false if it was used before; true otherwise, also when the registry is unavailable (the one-answer-per-
     *     question constraint in the database still holds)
     */
    boolean firstUse(UUID tokenId, Duration ttl);
}
