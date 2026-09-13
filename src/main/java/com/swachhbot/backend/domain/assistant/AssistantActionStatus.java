package com.swachhbot.backend.domain.assistant;

/** Lifecycle of a robot action the assistant has *proposed*. */
public enum AssistantActionStatus {
    /** Proposed by the LLM, waiting for a human to confirm. */
    PENDING,
    /** Confirmed and dispatched to the robot. */
    EXECUTED,
    /** Explicitly declined by the user. */
    REJECTED,
    /** Nobody confirmed it in time; it can never run. */
    EXPIRED
}
