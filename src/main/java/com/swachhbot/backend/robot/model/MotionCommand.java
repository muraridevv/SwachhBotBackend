package com.swachhbot.backend.robot.model;

import java.time.Duration;

/** Hardware-independent motion command. */
public record MotionCommand(
        double linearVelocity,
        double angularVelocity,
        Duration duration
) {
}
