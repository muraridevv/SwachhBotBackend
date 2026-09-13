package com.swachhbot.backend.domain.enums;

/** High-level robot operational state. */
public enum RobotStatus {
    IDLE,
    CLEANING,
    PAUSED,
    RETURNING,
    CHARGING,
    ERROR
}
