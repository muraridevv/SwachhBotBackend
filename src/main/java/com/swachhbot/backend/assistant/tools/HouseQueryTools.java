package com.swachhbot.backend.assistant.tools;

import com.swachhbot.backend.assistant.ActionProposalCollector;
import com.swachhbot.backend.domain.*;
import com.swachhbot.backend.domain.learning.LearnedInsightEntity;
import com.swachhbot.backend.learning.LearningService;
import com.swachhbot.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Read-only tools the assistant may call.
 *
 * <p>Every tool is bound to a single house by the server — the model cannot ask
 * about, or act on, a different house. Nothing here mutates state, so these are
 * always safe to call.
 */
@RequiredArgsConstructor
public class HouseQueryTools {

    private final UUID houseId;
    private final String robotId;
    private final ActionProposalCollector collector;

    private final HouseRepository houseRepository;
    private final RoomRepository roomRepository;
    private final RobotObjectRepository objectRepository;
    private final CleaningSessionRepository sessionRepository;
    private final ProblemAreaRepository problemAreaRepository;
    private final OccupancyMapRepository mapRepository;
    private final RobotStateRepository robotStateRepository;
    private final LearningService learningService;

    // ------------------------------------------------------------------ map

    @Tool(name = "getHouseMap",
            description = "Get a summary of the house layout: overall size, the rooms it contains, "
                    + "and how much of the floor has been mapped and cleaned.")
    public String getHouseMap() {
        collector.record("getHouseMap", "looked at the floor map");

        Optional<House> house = houseRepository.findById(houseId);
        List<Room> rooms = roomRepository.findByHouseId(houseId);

        StringBuilder sb = new StringBuilder();
        house.ifPresent(h -> sb.append("House '%s' is %.0f x %.0f units.\n"
                .formatted(h.getName(), h.getWidth(), h.getHeight())));
        sb.append("Rooms: ").append(
                rooms.stream().map(Room::getName).collect(Collectors.joining(", "))).append(".\n");

        mapRepository.findFirstByHouseIdOrderByUpdatedAtDesc(houseId).ifPresent(map -> {
            String data = map.getMapData();
            long cleaned = data.chars().filter(c -> c == 'C').count();
            long obstacles = data.chars().filter(c -> c == 'O').count();
            long unknown = data.chars().filter(c -> c == 'U').count();
            long total = Math.max(1, data.length());
            sb.append("Map grid %dx%d, cell size %.0f. Cleared: %.1f%%, obstacles: %.1f%%, unexplored: %.1f%%."
                    .formatted(map.getGridWidth(), map.getGridHeight(), map.getCellSize(),
                            cleaned * 100.0 / total, obstacles * 100.0 / total, unknown * 100.0 / total));
        });
        return sb.toString();
    }

    // ----------------------------------------------------------------- room

    @Tool(name = "getRoomInformation",
            description = "Get details about one room: its position, size, the furniture inside it, "
                    + "and anything the robot has learned about it.")
    public String getRoomInformation(
            @ToolParam(description = "Exact room name, e.g. Kitchen") String roomName) {
        collector.record("getRoomInformation", "checked room '" + roomName + "'");

        List<Room> rooms = roomRepository.findByHouseId(houseId);
        Optional<Room> match = rooms.stream()
                .filter(r -> r.getName().equalsIgnoreCase(roomName)
                        || r.getName().toLowerCase(Locale.ROOT)
                        .contains(roomName.toLowerCase(Locale.ROOT)))
                .findFirst();

        if (match.isEmpty()) {
            return "No room named '%s'. Known rooms: %s.".formatted(roomName,
                    rooms.stream().map(Room::getName).collect(Collectors.joining(", ")));
        }

        Room room = match.get();
        StringBuilder sb = new StringBuilder();
        sb.append("Room '%s' is at (%.0f, %.0f), size %.0f x %.0f.\n".formatted(
                room.getName(), room.getX(), room.getY(), room.getWidth(), room.getHeight()));
        sb.append("Furniture: ").append(room.getFurniture().isEmpty() ? "none"
                : room.getFurniture().stream().map(Furniture::getType).collect(Collectors.joining(", ")))
                .append(".\n");

        List<LearnedInsightEntity> insights = learningService.getInsights(houseId).stream()
                .filter(i -> i.getSubjectLabel().equalsIgnoreCase(room.getName()))
                .toList();
        if (!insights.isEmpty()) {
            sb.append("Learned: ").append(insights.stream()
                    .map(i -> i.getSummary() + " (confidence %.0f%%)".formatted(i.getConfidence() * 100))
                    .collect(Collectors.joining(" ")));
        }
        return sb.toString();
    }

    // -------------------------------------------------------------- history

    @Tool(name = "getCleaningHistory",
            description = "Get the most recent cleaning sessions: when they ran, how long they took "
                    + "and what percentage of the floor was cleaned.")
    public String getCleaningHistory(
            @ToolParam(description = "How many recent sessions to return, e.g. 5") Integer limit) {
        int count = (limit == null || limit <= 0) ? 5 : Math.min(limit, 20);
        collector.record("getCleaningHistory", "read the last %d session(s)".formatted(count));

        List<CleaningSession> sessions = sessionRepository.findByHouseIdOrderByStartedAtDesc(houseId);
        if (sessions.isEmpty()) {
            return "No cleaning sessions recorded yet.";
        }
        return sessions.stream().limit(count)
                .map(s -> "%s: %.0fs, %.1f%% of the floor covered"
                        .formatted(s.getStartedAt(), (double) s.getDurationSeconds(), s.getCleanedPercentage()))
                .collect(Collectors.joining("\n"));
    }

    // --------------------------------------------------------------- status

    @Tool(name = "getRobotStatus",
            description = "Get the robot's current state: position, battery level and whether it is "
                    + "idle, cleaning, paused, returning or charging.")
    public String getRobotStatus() {
        collector.record("getRobotStatus", "checked the robot's status");
        return robotStateRepository.findByRobotId(robotId)
                .map(s -> "Robot %s is %s at (%.0f, %.0f), battery %.0f%%, speed %.0f."
                        .formatted(s.getRobotId(), s.getStatus(), s.getX(), s.getY(),
                                s.getBattery(), s.getVelocity()))
                .orElse("I have no live status for robot '%s' yet.".formatted(robotId));
    }

    // ------------------------------------------------------- learned memory

    @Tool(name = "getLearnedKnowledge",
            description = "Get everything the robot has learned about this house: dirty areas, "
                    + "places it gets stuck, temporary objects, cleaning patterns and room habits.")
    public String getLearnedKnowledge() {
        collector.record("getLearnedKnowledge", "reviewed what I have learned");

        List<LearnedInsightEntity> insights = learningService.getInsights(houseId);
        if (insights.isEmpty()) {
            return "I have not learned anything about this house yet.";
        }
        return insights.stream()
                .map(i -> "- [%s, confidence %.0f%%] %s"
                        .formatted(i.getCategory().name().toLowerCase(Locale.ROOT),
                                i.getConfidence() * 100, i.getSummary()))
                .collect(Collectors.joining("\n"));
    }

    @Tool(name = "getFrequentlyDirtyRooms",
            description = "Get the rooms that most often need extra cleaning.")
    public String getFrequentlyDirtyRooms() {
        collector.record("getFrequentlyDirtyRooms", "ranked the dirtiest rooms");
        return learningService.getInsights(houseId).stream()
                .filter(i -> i.getCategory().name().equals("DIRTY_AREA"))
                .map(i -> "%s — %s (confidence %.0f%%)"
                        .formatted(i.getSubjectLabel(), i.getSummary(), i.getConfidence() * 100))
                .collect(Collectors.collectingAndThen(Collectors.joining("\n"),
                        s -> s.isBlank() ? "No room stands out as consistently dirty." : s));
    }

    @Tool(name = "getStuckLocations",
            description = "Get the places where the robot tends to get stuck or blocked, and when it "
                    + "last happened.")
    public String getStuckLocations() {
        collector.record("getStuckLocations", "looked up where I get stuck");
        String learned = learningService.getInsights(houseId).stream()
                .filter(i -> i.getCategory().name().equals("BLOCKED_AREA"))
                .map(i -> "%s — %s".formatted(i.getSubjectLabel(), i.getSummary()))
                .collect(Collectors.joining("\n"));

        String reported = problemAreaRepository.findByHouseIdOrderByFrequencyDesc(houseId).stream()
                .map(p -> "%s at (%.0f, %.0f), %d time(s), last %s"
                        .formatted(p.getDescription(), p.getX(), p.getY(), p.getFrequency(), p.getLastSeen()))
                .collect(Collectors.joining("\n"));

        String combined = (learned + "\n" + reported).trim();
        return combined.isBlank() ? "The robot has not reported getting stuck anywhere." : combined;
    }

    @Tool(name = "getObjectHistory",
            description = "Get the objects the robot has seen in the house, and whether they are "
                    + "permanent furniture or things that tend to move.")
    public String getObjectHistory() {
        collector.record("getObjectHistory", "reviewed detected objects");
        List<RobotObject> objects = objectRepository.findByHouseId(houseId);
        if (objects.isEmpty()) {
            return "I have not detected any objects yet.";
        }
        return objects.stream()
                .map(o -> "%s (%s, %s) seen %d time(s)".formatted(
                        o.getType(), o.getCategory(), o.getStatus(), o.getDetectionCount()))
                .collect(Collectors.joining("\n"));
    }
}
