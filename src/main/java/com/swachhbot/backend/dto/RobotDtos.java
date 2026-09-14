package com.swachhbot.backend.dto;

import com.swachhbot.backend.domain.enums.CommandStatus;
import com.swachhbot.backend.domain.enums.CommandType;
import com.swachhbot.backend.domain.enums.RobotStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/** DTOs for live robot state, telemetry and commands. */
public final class RobotDtos {

    private RobotDtos() {
    }

    /** Snapshot of the robot (REST read/write + WebSocket payload). */
    public record RobotStateDto(
            String robotId,
            UUID houseId,
            double x,
            double y,
            double rotation,
            double velocity,
            double battery,
            RobotStatus status,
            boolean isColliding,
            Instant updatedAt
    ) {
    }

    /** Real-time telemetry frame published over WebSocket. */
    public record TelemetryMessage(
            String type,          // e.g. "telemetry"
            String robotId,
            double x,
            double y,
            double rotation,
            double velocity,
            double battery,
            RobotStatus status,
            boolean isColliding,
            Double cleaningPercent,
            Instant timestamp
    ) {
        public static final String TYPE = "telemetry";
    }

    /** Real-time command frame sent to the robot computer. */
    public record RobotCommandMessage(
            String type,          // e.g. "command"
            String robotId,
            String commandType,   // e.g. "MOVE", "STOP"
            String payload,       // e.g. json with linear/angular velocity
            Instant timestamp
    ) {
        public static final String TYPE = "command";
    }

    // ----- Commands -----

    public record CommandDto(
            UUID id,
            String robotId,
            UUID houseId,
            CommandType command,
            CommandStatus status,
            String payload,
            Instant issuedAt,
            Instant ackedAt
    ) {
    }

    public record CommandRequest(
            @NotBlank String robotId,
            UUID houseId,
            @NotNull CommandType command,
            String payload
    ) {
    }

    public record CommandAckRequest(
            @NotNull CommandStatus status
    ) {
    }
}
