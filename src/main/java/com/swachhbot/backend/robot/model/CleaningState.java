package com.swachhbot.backend.robot.model;

import java.util.UUID;

public record CleaningState(
        UUID currentSessionId,
        String currentRoomName,
        double cleanedPercentage,
        double areaCleanedSqm,
        long elapsedTimeSeconds
) {
}
