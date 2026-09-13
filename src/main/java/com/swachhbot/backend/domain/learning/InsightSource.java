package com.swachhbot.backend.domain.learning;

/** Where an insight came from. User-sourced insights always win. */
public enum InsightSource {
    /** Derived automatically from session / object / map history. */
    DERIVED,
    /** Asserted by a human. */
    USER
}
