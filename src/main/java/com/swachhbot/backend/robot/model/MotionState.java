package com.swachhbot.backend.robot.model;

/** Detailed real-time motion data. */
public record MotionState(
        double linearVelocity,
        double angularVelocity,
        double acceleration,
        boolean isColliding
) {
}
