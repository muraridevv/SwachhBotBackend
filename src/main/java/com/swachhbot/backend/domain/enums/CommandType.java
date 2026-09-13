package com.swachhbot.backend.domain.enums;

/** Commands the backend can dispatch to a robot (Android app or Pi/ROS). */
public enum CommandType {
    START_CLEANING,
    PAUSE,
    RESUME,
    STOP,
    RETURN_TO_DOCK,
    GO_TO,
    RESET_MAP,
    MOVE,
    START_EXPLORATION
}
