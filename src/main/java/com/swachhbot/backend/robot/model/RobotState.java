package com.swachhbot.backend.robot.model;

import com.swachhbot.backend.domain.enums.RobotStatus;
import java.time.Instant;
import java.util.UUID;

public record RobotState(
        String robotId,
        UUID houseId,
        RobotPosition position,
        Orientation orientation,
        double velocity,
        BatteryState battery,
        RobotStatus status,
        Instant timestamp
) {
}
