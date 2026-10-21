package com.doping.prescription.feed.domain;

/** What the learner did with a card (docs/arayuz.md, interaction events). */
public enum InteractionType {
    VIEW_START,
    VIEW_COMPLETE,
    SKIP,
    LIKE,
    QUIZ_SHOWN,
    QUIZ_DISMISSED
}
