package com.swachhbot.backend.dto;

import com.swachhbot.backend.domain.enums.ObjectCategory;
import com.swachhbot.backend.domain.enums.ObjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/** DTOs for remembered objects. */
public final class ObjectDtos {

    private ObjectDtos() {
    }

    public record ObjectDto(
            UUID id,
            UUID houseId,
            String type,
            ObjectCategory category,
            ObjectStatus status,
            String roomName,
            double x,
            double y,
            double confidence,
            Instant firstDetected,
            Instant lastDetected,
            int detectionCount
    ) {
    }

    /**
     * Upsert sent by the client. The {@code id} is client-generated so the
     * Android local database and backend stay in sync without extra round-trips.
     */
    public record ObjectUpsertRequest(
            @NotNull UUID id,
            @NotBlank String type,
            @NotNull ObjectCategory category,
            @NotNull ObjectStatus status,
            String roomName,
            double x,
            double y,
            double confidence,
            Instant firstDetected,
            Instant lastDetected,
            int detectionCount
    ) {
    }
}
