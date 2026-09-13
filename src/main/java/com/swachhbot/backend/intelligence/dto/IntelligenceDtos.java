package com.swachhbot.backend.intelligence.dto;

import java.util.List;
import java.util.UUID;

public final class IntelligenceDtos {

    private IntelligenceDtos() {
    }

    public record RoomPriorityScore(
            UUID roomId,
            String roomName,
            double totalScore,
            double dirtScore,
            double recencyScore,
            int userPriority,
            String explanation
    ) {
    }

    public record HouseCleaningStrategy(
            UUID houseId,
            List<RoomPriorityScore> rankedRooms,
            String summary
    ) {
    }
}
