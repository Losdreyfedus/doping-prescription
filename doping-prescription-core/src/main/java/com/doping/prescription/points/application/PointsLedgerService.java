package com.doping.prescription.points.application;

import com.doping.prescription.points.api.PointsLedger;
import org.springframework.stereotype.Service;

/** Applies the points rules of docs/algoritma.md §7. */
@Service
class PointsLedgerService implements PointsLedger {

    private final PointsProperties properties;

    PointsLedgerService(PointsProperties properties) {
        this.properties = properties;
    }

    @Override
    public int pointsAvailable(boolean weakTopic) {
        return weakTopic ? properties.weakTopicCorrect() : properties.correct();
    }
}
