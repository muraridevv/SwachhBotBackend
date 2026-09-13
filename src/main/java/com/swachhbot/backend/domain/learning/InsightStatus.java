package com.swachhbot.backend.domain.learning;

/** How much we trust an insight, and whether a human has weighed in. */
public enum InsightStatus {
    /** Seen once or twice; may be noise. */
    EMERGING,
    /** Seen often enough to act on. */
    CONFIRMED,
    /** A human explicitly agreed with it. */
    USER_CONFIRMED,
    /** A human explicitly rejected it; the robot must not act on it. */
    USER_REJECTED
}
