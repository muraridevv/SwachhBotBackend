package com.swachhbot.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/** DTOs for cleaning sessions and problem areas. */
public final class SessionDtos {

    private SessionDtos() {
    }

    public record SessionDto(
            UUID id,
            UUID houseId,
            UUID roomId,
            Instant startedAt,
            Instant endedAt,
            long durationSeconds,
            double cleanedPercentage,
            double areaCleanedSqm
    ) {
    }

    public record SessionRequest(
            UUID roomId,
            @NotNull Instant startedAt,
            Instant endedAt,
            long durationSeconds,
            double cleanedPercentage,
            double areaCleanedSqm
    ) {
    }

    // ----- Problem areas -----

    public record ProblemAreaDto(
            UUID id,
            UUID houseId,
            double x,
            double y,
            double radius,
            String description,
            int frequency,
            Instant lastSeen
    ) {
    }

    public record ProblemAreaRequest(
            double x,
            double y,
            double radius,
            @NotBlank String description
    ) {
    }
}
