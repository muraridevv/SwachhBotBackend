package com.swachhbot.backend.learning.analyzer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swachhbot.backend.domain.ProblemArea;
import com.swachhbot.backend.domain.Room;
import com.swachhbot.backend.domain.learning.InsightCategory;
import com.swachhbot.backend.domain.plan.CleaningPlan;
import com.swachhbot.backend.domain.plan.CleaningPlanEntity;
import com.swachhbot.backend.learning.InsightAnalyzer;
import com.swachhbot.backend.repository.CleaningPlanRepository;
import com.swachhbot.backend.repository.ProblemAreaRepository;
import com.swachhbot.backend.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Learns per-room habits from the history of executed plans.
 *
 * <ul>
 *   <li><b>CLEANING_PATTERN</b> — "The kitchen usually requires two passes."</li>
 *   <li><b>ROOM_CLEANLINESS</b> — "The hallway has remained clean during the last five sessions."</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RoomHabitAnalyzer implements InsightAnalyzer {

    private static final int MIN_REPEATS = 2;
    private static final int RECENT_PLANS = 5;

    private final CleaningPlanRepository planRepository;
    private final RoomRepository roomRepository;
    private final ProblemAreaRepository problemAreaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<InsightDraft> analyze(UUID houseId) {
        List<Room> rooms = roomRepository.findByHouseId(houseId);
        if (rooms.isEmpty()) {
            return List.of();
        }
        Map<UUID, Room> byId = new HashMap<>();
        rooms.forEach(r -> byId.put(r.getId(), r));

        List<CleaningPlanEntity> completed = planRepository.findByHouseIdOrderByCreatedAtDesc(houseId).stream()
                .filter(p -> CleaningPlan.Status.COMPLETED.name().equals(p.getStatus()))
                .toList();
        if (completed.isEmpty()) {
            return List.of();
        }

        List<ProblemArea> problems = problemAreaRepository.findByHouseIdOrderByFrequencyDesc(houseId);
        List<InsightDraft> drafts = new ArrayList<>();

        // ---- Passes per room ------------------------------------------------
        Map<UUID, List<Integer>> passesByRoom = new HashMap<>();
        for (CleaningPlanEntity plan : completed) {
            for (UUID roomId : roomIdsOf(plan)) {
                passesByRoom.computeIfAbsent(roomId, k -> new ArrayList<>()).add(plan.getPasses());
            }
        }
        passesByRoom.forEach((roomId, passes) -> {
            Room room = byId.get(roomId);
            if (room == null || passes.size() < MIN_REPEATS) {
                return;
            }
            int mode = mostCommon(passes);
            long occurrences = passes.stream().filter(p -> p == mode).count();
            if (occurrences < MIN_REPEATS) {
                return;
            }
            drafts.add(new InsightDraft(
                    InsightCategory.CLEANING_PATTERN,
                    "ROOM:PASSES:" + room.getName().toUpperCase(Locale.ROOT),
                    room.getName(),
                    "The %s usually requires %d pass%s.".formatted(
                            room.getName(), mode, mode == 1 ? "" : "es"),
                    (int) occurrences,
                    "{\"roomName\":\"%s\",\"passes\":%d}".formatted(room.getName(), mode)
            ));
        });

        // ---- Rooms that stay clean -----------------------------------------
        List<CleaningPlanEntity> recent = completed.subList(0, Math.min(RECENT_PLANS, completed.size()));
        Map<UUID, Integer> recentAppearances = new HashMap<>();
        for (CleaningPlanEntity plan : recent) {
            roomIdsOf(plan).forEach(roomId ->
                    recentAppearances.merge(roomId, 1, Integer::sum));
        }
        recentAppearances.forEach((roomId, count) -> {
            Room room = byId.get(roomId);
            if (room == null) {
                return;
            }
            boolean problematic = problems.stream().anyMatch(p ->
                    p.getX() >= room.getX() && p.getX() <= room.getX() + room.getWidth()
                            && p.getY() >= room.getY() && p.getY() <= room.getY() + room.getHeight());
            if (!problematic) {
                drafts.add(new InsightDraft(
                        InsightCategory.ROOM_CLEANLINESS,
                        "ROOM:CLEAN:" + room.getName().toUpperCase(Locale.ROOT),
                        room.getName(),
                        "The %s has stayed clean across the last %d session%s."
                                .formatted(room.getName(), count, count == 1 ? "" : "s"),
                        count,
                        "{\"roomName\":\"%s\"}".formatted(room.getName())
                ));
            }
        });

        return drafts;
    }

    private List<UUID> roomIdsOf(CleaningPlanEntity plan) {
        if (plan.getRoomsJson() == null || plan.getRoomsJson().isBlank()) {
            return List.of();
        }
        try {
            List<CleaningPlan.PlannedRoom> rooms =
                    objectMapper.readValue(plan.getRoomsJson(), new TypeReference<>() {
                    });
            return rooms.stream()
                    .filter(CleaningPlan.PlannedRoom::cleanRequired)
                    .map(CleaningPlan.PlannedRoom::roomId)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.debug("Skipping unreadable plan rooms JSON", e);
            return List.of();
        }
    }

    private int mostCommon(List<Integer> values) {
        Map<Integer, Integer> counts = new HashMap<>();
        values.forEach(v -> counts.merge(v, 1, Integer::sum));
        return counts.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(1);
    }
}
