package com.doping.prescription.points.api;

/** The points rules and ledger (docs/algoritma.md §7). */
public interface PointsLedger {

    /** Points a correct answer earns, shown on the card before the learner answers. */
    int pointsAvailable(boolean weakTopic);
}
