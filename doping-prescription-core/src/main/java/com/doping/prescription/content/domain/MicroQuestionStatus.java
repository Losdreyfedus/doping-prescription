package com.doping.prescription.content.domain;

/** Lifecycle of a micro-question; whether it is shown also depends on its video's status. */
public enum MicroQuestionStatus {
    DRAFT,
    LIVE,
    RETIRED
}
