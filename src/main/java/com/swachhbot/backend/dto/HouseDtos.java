package com.swachhbot.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

/** DTOs for houses, rooms and furniture. */
public final class HouseDtos {

    private HouseDtos() {
    }

    // ----- House -----

    public record HouseDto(
            UUID id,
            String name,
            double width,
            double height,
            List<RoomDto> rooms
    ) {
    }

    public record HouseRequest(
            @NotBlank String name,
            @Positive double width,
            @Positive double height
    ) {
    }

    // ----- Room -----

    public record RoomDto(
            UUID id,
            String name,
            double x,
            double y,
            double width,
            double height,
            List<FurnitureDto> furniture
    ) {
    }

    public record RoomRequest(
            @NotBlank String name,
            double x,
            double y,
            @Positive double width,
            @Positive double height
    ) {
    }

    // ----- Furniture -----

    public record FurnitureDto(
            UUID id,
            String type,
            double x,
            double y,
            double width,
            double height,
            double rotationDeg
    ) {
    }

    public record FurnitureRequest(
            @NotBlank String type,
            double x,
            double y,
            @Positive double width,
            @Positive double height,
            double rotationDeg
    ) {
    }
}
