package com.doping.prescription.learner.domain;

/** Kind of learning evidence; weights and effects are in docs/algoritma.md §1. */
public enum SignalType {
    EXAM_CORRECT,
    EXAM_WRONG,
    EXAM_BLANK,
    QUIZ_CORRECT,
    QUIZ_WRONG,
    VIDEO_COMPLETED,
    VIDEO_SKIPPED
}
