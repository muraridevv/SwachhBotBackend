package com.swachhbot.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.UUID;

/** DTOs for the serialized occupancy map. */
public final class MapDtos {

    private MapDtos() {
    }

    public record MapDto(
            UUID id,
            UUID houseId,
            int gridWidth,
            int gridHeight,
            double cellSize,
            String mapData,
            long version,
            Instant updatedAt
    ) {
    }

    public record MapUpdateRequest(
            @Positive int gridWidth,
            @Positive int gridHeight,
            @Positive double cellSize,
            @NotBlank String mapData
    ) {
    }
}
