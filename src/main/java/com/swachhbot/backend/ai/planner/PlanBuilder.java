package com.swachhbot.backend.ai.planner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swachhbot.backend.ai.config.AiProperties;
import com.swachhbot.backend.ai.dto.StructuredCommand;
import com.swachhbot.backend.domain.ProblemArea;
import com.swachhbot.backend.domain.Room;
import com.swachhbot.backend.domain.learning.LearnedInsightEntity;
import com.swachhbot.backend.domain.plan.CleaningPlan;
import com.swachhbot.backend.domain.plan.CleaningPlanEntity;
import com.swachhbot.backend.learning.LearningService;
import com.swachhbot.backend.repository.CleaningPlanRepository;
import com.swachhbot.backend.repository.ProblemAreaRepository;
import com.swachhbot.backend.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the LLM's {@link StructuredCommand} into a concrete {@link CleaningPlan}
 * <b>deterministically</b>.
 *
 * <p>This is the heart of the safety design: the model only classifies intent.
 * Every concrete decision — which rooms exist, what order to visit them, how
 * long it takes, what "3 days" means — is computed here from the database.
 * An LLM hallucination can therefore at worst select a wrong *category*, never
 * emit an unverified action.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlanBuilder {

    /** Rough per-room-per-pass time used for user-facing estimates only. */
    private static final long BASE_SECONDS_PER_ROOM_PASS = 90;
    private static final Pattern TIMEFRAME = Pattern.compile("(\\d+)\\s*(hour|day|week)", Pattern.CASE_INSENSITIVE);

    private final RoomRepository roomRepository;
    private final ProblemAreaRepository problemAreaRepository;
    private final CleaningPlanRepository planRepository;
    private final LearningService learningService;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    public CleaningPlan build(UUID houseId, String naturalLanguage, StructuredCommand cmd, boolean aiGenerated) {
        List<Room> allRooms = roomRepository.findByHouseId(houseId);
        Map<String, Room> byName = new HashMap<>();
        for (Room room : allRooms) {
            byName.put(room.getName().toUpperCase(Locale.ROOT), room);
        }

        CleaningPlan.Action action = parseAction(cmd.action());
        CleaningPlan.Priority priority = parsePriority(cmd.priority());
        Set<String> excluded = normalizeNames(cmd.excludedRooms());
        List<String> excludedKnown = excluded.stream()
                .filter(byName::containsKey)
                .toList();

        // ---- 1. Select candidate rooms --------------------------------------
        List<Room> candidates = switch (action) {
            case CLEAN -> resolveNamed(cmd.rooms(), byName);
            case CLEAN_ALL -> new ArrayList<>(allRooms);
            case CLEAN_EXCEPT -> allRooms.stream()
                    .filter(r -> !excluded.contains(r.getName().toUpperCase(Locale.ROOT)))
                    .toList();
        };

        // ---- 2. Apply the recency filter ("not cleaned in 3 days") ----------
        Duration staleness = parseTimeframe(cmd.timeframe());
        if (staleness != null) {
            Instant threshold = Instant.now().minus(staleness);
            candidates = candidates.stream()
                    .filter(r -> lastCleanedAt(houseId, r.getId()).map(t -> t.isBefore(threshold)).orElse(true))
                    .toList();
        }

        // ---- 3. Order: dirtiest / most problematic first --------------------
        Map<UUID, Integer> problemScore = problemScoresByRoom(houseId, allRooms);
        boolean dirtiestFirst = priority == CleaningPlan.Priority.HIGH
                || priority == CleaningPlan.Priority.URGENT
                || mentionsDirt(cmd.reasoning());
        List<Room> ordered = new ArrayList<>(candidates);
        if (dirtiestFirst) {
            ordered.sort(Comparator.comparingInt((Room r) -> problemScore.getOrDefault(r.getId(), 0)).reversed());
        }

        // ---- 4. Consult what the robot has learned --------------------------
        Map<String, LearnedInsightEntity> learned = new HashMap<>();
        learningService.getActionable(houseId).forEach(i ->
                learned.put(i.getCategory().name() + ":" + i.getSubjectKey(), i));

        // ---- 5. Materialise planned rooms -----------------------------------
        int learnedPasses = 1;
        List<CleaningPlan.PlannedRoom> planned = new ArrayList<>();
        int order = 1;
        for (Room room : ordered) {
            String upper = room.getName().toUpperCase(Locale.ROOT);
            int score = problemScore.getOrDefault(room.getId(), 0);

            LearnedInsightEntity dirtyInsight = learned.get("DIRTY_AREA:ROOM:" + upper);
            LearnedInsightEntity cleanInsight = learned.get("ROOM_CLEANLINESS:ROOM:CLEAN:" + upper);
            LearnedInsightEntity passInsight = learned.get("CLEANING_PATTERN:ROOM:PASSES:" + upper);

            if (passInsight != null) {
                learnedPasses = Math.max(learnedPasses, extractPasses(passInsight.getDetails()));
            }

            CleaningPlan.Priority roomPriority;
            String rationale;
            if (score > 0) {
                roomPriority = CleaningPlan.Priority.HIGH;
                rationale = "flagged %d time(s) as a problem area".formatted(score);
            } else if (dirtyInsight != null) {
                roomPriority = CleaningPlan.Priority.HIGH;
                rationale = "learned: %s".formatted(dirtyInsight.getSummary());
            } else if (cleanInsight != null) {
                roomPriority = CleaningPlan.Priority.LOW;
                rationale = "learned: %s".formatted(cleanInsight.getSummary());
            } else {
                roomPriority = priority;
                rationale = "included by request";
            }
            planned.add(new CleaningPlan.PlannedRoom(
                    room.getId(), room.getName(), order++, roomPriority, true, rationale));
        }

        // Learned habit raises the pass count (still bounded by the validator).
        int requested = cmd.passes() == null ? 1 : cmd.passes();
        int passes = clampPasses(Math.max(requested, learnedPasses));
        // Always describe exclusions, even if they are not cleaned.
        for (String name : excludedKnown) {
            Room room = byName.get(name);
            planned.add(new CleaningPlan.PlannedRoom(
                    room.getId(), room.getName(), order++, CleaningPlan.Priority.LOW, false,
                    "excluded by request"));
        }

        long estimated = Math.max(1, (long) ordered.size() * passes * BASE_SECONDS_PER_ROOM_PASS);
        String reason = buildReason(action, ordered, excludedKnown, staleness, cmd.reasoning());

        return new CleaningPlan(
                UUID.randomUUID(),
                houseId,
                naturalLanguage,
                action,
                planned,
                priority,
                passes,
                excludedKnown,
                estimated,
                reason,
                aiGenerated,
                CleaningPlan.Status.DRAFT,
                Instant.now()
        );
    }

    // ----- helpers -----------------------------------------------------------

    private CleaningPlan.Action parseAction(String raw) {
        if (raw == null || raw.isBlank()) {
            return CleaningPlan.Action.CLEAN_ALL;
        }
        try {
            return CleaningPlan.Action.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warn("LLM returned unknown action '{}', defaulting to CLEAN_ALL", raw);
            return CleaningPlan.Action.CLEAN_ALL;
        }
    }

    private CleaningPlan.Priority parsePriority(String raw) {
        if (raw == null || raw.isBlank()) {
            return CleaningPlan.Priority.NORMAL;
        }
        try {
            return CleaningPlan.Priority.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warn("LLM returned unknown priority '{}', defaulting to NORMAL", raw);
            return CleaningPlan.Priority.NORMAL;
        }
    }

    private List<Room> resolveNamed(List<String> names, Map<String, Room> byName) {
        if (names == null) {
            return List.of();
        }
        List<Room> rooms = new ArrayList<>();
        for (String name : normalizeNames(names)) {
            Room exact = byName.get(name);
            if (exact != null) {
                rooms.add(exact);
                continue;
            }
            // Tolerate "living room" vs "LIVING_ROOM" and partial matches.
            byName.entrySet().stream()
                    .filter(e -> e.getKey().replace('_', ' ').contains(name.replace('_', ' ')))
                    .findFirst()
                    .ifPresent(e -> rooms.add(e.getValue()));
        }
        return rooms;
    }

    private Set<String> normalizeNames(List<String> names) {
        if (names == null) {
            return Set.of();
        }
        Set<String> out = new LinkedHashSet<>();
        for (String name : names) {
            if (name != null && !name.isBlank()) {
                out.add(name.trim().toUpperCase(Locale.ROOT));
            }
        }
        return out;
    }

    private int clampPasses(Integer requested) {
        int passes = requested == null ? 1 : requested;
        return Math.max(1, Math.min(passes, properties.getMaxPasses()));
    }

    /** Parses "3 days" / "1 week" / "12 hours" into a {@link Duration}. */
    Duration parseTimeframe(String timeframe) {
        if (timeframe == null || timeframe.isBlank()) {
            return null;
        }
        Matcher matcher = TIMEFRAME.matcher(timeframe);
        if (!matcher.find()) {
            return null;
        }
        long amount = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2).toLowerCase(Locale.ROOT);
        return switch (unit) {
            case "hour" -> Duration.ofHours(amount);
            case "day" -> Duration.ofDays(amount);
            case "week" -> Duration.ofDays(amount * 7);
            default -> null;
        };
    }

    private Optional<Instant> lastCleanedAt(UUID houseId, UUID roomId) {
        return planRepository.findByHouseIdOrderByCreatedAtDesc(houseId).stream()
                .filter(p -> CleaningPlan.Status.COMPLETED.name().equals(p.getStatus()))
                .filter(p -> p.getRoomsJson() != null && p.getRoomsJson().contains(roomId.toString()))
                .map(CleaningPlanEntity::getCreatedAt)
                .findFirst();
    }

    private Map<UUID, Integer> problemScoresByRoom(UUID houseId, List<Room> rooms) {
        List<ProblemArea> problems = problemAreaRepository.findByHouseIdOrderByFrequencyDesc(houseId);
        Map<UUID, Integer> scores = new HashMap<>();
        for (Room room : rooms) {
            int score = 0;
            for (ProblemArea problem : problems) {
                if (problem.getX() >= room.getX() && problem.getX() <= room.getX() + room.getWidth()
                        && problem.getY() >= room.getY() && problem.getY() <= room.getY() + room.getHeight()) {
                    score += problem.getFrequency();
                }
            }
            if (score > 0) {
                scores.put(room.getId(), score);
            }
        }
        return scores;
    }

    private boolean mentionsDirt(String reasoning) {
        if (reasoning == null) {
            return false;
        }
        String r = reasoning.toLowerCase(Locale.ROOT);
        return r.contains("dirt") || r.contains("dust") || r.contains("mess") || r.contains("stain");
    }

    private String buildReason(CleaningPlan.Action action, List<Room> rooms, List<String> excluded,
                               Duration staleness, String llmReasoning) {
        StringBuilder sb = new StringBuilder();
        sb.append(switch (action) {
            case CLEAN -> "Requested rooms: ";
            case CLEAN_ALL -> "Cleaning the whole house. Rooms: ";
            case CLEAN_EXCEPT -> "Cleaning all rooms except " + excluded + ". Rooms: ";
        });
        sb.append(rooms.stream().map(Room::getName).toList());
        if (staleness != null) {
            long hours = staleness.toHours();
            String human = hours >= 24 ? (hours / 24) + " day(s)" : hours + " hour(s)";
            sb.append(". Filtered to rooms untouched for the last ").append(human);
        }
        sb.append('.');
        if (llmReasoning != null && !llmReasoning.isBlank()) {
            sb.append(' ').append(llmReasoning);
        }
        return sb.toString();
    }

    /** Pulls {@code "passes":N} out of a stored insight's details JSON. */
    private int extractPasses(String details) {
        if (details == null) {
            return 1;
        }
        Matcher m = Pattern
                .compile("\"passes\"\\s*:\\s*(\\d+)")
                .matcher(details);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        return 1;
    }

    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }
}
