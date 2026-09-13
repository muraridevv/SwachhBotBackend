package com.swachhbot.backend.robot.model;

import java.time.Instant;
import java.util.UUID;

public record RobotCommandResult(
    UUID commandId,
    String status,
    Instant timestamp
) {
}
