package com.swachhbot.backend.ai.execution;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swachhbot.backend.ai.planner.PlanValidationException;
import com.swachhbot.backend.ai.planner.PlanValidator;
import com.swachhbot.backend.domain.Room;
import com.swachhbot.backend.domain.enums.CommandType;
import com.swachhbot.backend.domain.plan.CleaningPlan;
import com.swachhbot.backend.domain.plan.CleaningPlanEntity;
import com.swachhbot.backend.dto.RobotDtos.CommandDto;
import com.swachhbot.backend.dto.RobotDtos.CommandRequest;
import com.swachhbot.backend.repository.CleaningPlanRepository;
import com.swachhbot.backend.repository.RoomRepository;
import com.swachhbot.backend.service.CommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Converts a <b>validated</b> plan into robot commands.
 *
 * <h2>Two safety properties</h2>
 * <ol>
 *   <li><b>Re-validation at execution time.</b> The plan is re-loaded from the
 *       database and re-checked against {@link PlanValidator}. A plan posted by
 *       a client is never trusted — only the server's own persisted copy is.</li>
 *   <li><b>High-level commands only.</b> This class emits {@link CommandType}
 *       values. It never computes or transmits motor speeds, angles or raw
 *       coordinates — that stays inside the robot's navigation engine.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlanExecutionService {

    private final CommandService commandService;
    private final CleaningPlanRepository planRepository;
    private final RoomRepository roomRepository;
    private final PlanValidator planValidator;
    private final ObjectMapper objectMapper;

    @Transactional
    public CommandDto execute(UUID planId, String robotId) {
        CleaningPlan plan = loadValidated(planId);

        List<Map<String, Object>> roomOrders = plan.rooms().stream()
                .filter(CleaningPlan.PlannedRoom::cleanRequired)
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("order", r.order());
                    m.put("roomId", r.roomId());
                    m.put("name", r.name());
                    m.put("priority", r.priority().name());
                    return m;
                })
                .toList();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("planId", plan.planId());
        payload.put("action", plan.action().name());
        payload.put("priority", plan.priority().name());
        payload.put("passes", plan.passes());
        payload.put("excludedAreas", plan.excludedAreas());
        payload.put("roomOrder", roomOrders);
        payload.put("estimatedDurationSeconds", plan.estimatedDurationSeconds());
        payload.put("reason", plan.reason());

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new PlanValidationException("Could not serialize plan payload");
        }

        // HIGH-LEVEL COMMAND ONLY. Navigation is the robot's responsibility.
        CommandDto command = commandService.issue(
                new CommandRequest(robotId, plan.houseId(), CommandType.START_CLEANING, json));

        markStatus(planId, CleaningPlan.Status.EXECUTING);
        log.info("Dispatched plan {} to robot {} as command {}", planId, robotId, command.id());
        return command;
    }

    /** Called by the robot (or operator) when the run finishes. */
    @Transactional
    public void complete(UUID planId) {
        markStatus(planId, CleaningPlan.Status.COMPLETED);
    }

    @Transactional
    public void reject(UUID planId) {
        markStatus(planId, CleaningPlan.Status.REJECTED);
    }

    // ----- internals ---------------------------------------------------------

    /** Rebuilds a plan from persisted state and re-runs the safety gate. */
    public CleaningPlan loadValidated(UUID planId) {
        CleaningPlanEntity entity = planRepository.findById(planId)
                .orElseThrow(() -> new PlanValidationException("Unknown plan: " + planId));

        List<CleaningPlan.PlannedRoom> rooms;
        try {
            rooms = objectMapper.readValue(entity.getRoomsJson(), new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new PlanValidationException("Stored plan is corrupt: " + planId);
        }

        CleaningPlan plan = new CleaningPlan(
                entity.getId(),
                entity.getHouseId(),
                entity.getNaturalLanguage(),
                CleaningPlan.Action.valueOf(entity.getAction()),
                rooms,
                CleaningPlan.Priority.valueOf(entity.getPriority()),
                entity.getPasses(),
                entity.getExcludedAreas() == null || entity.getExcludedAreas().isBlank()
                        ? List.of()
                        : List.of(entity.getExcludedAreas().split(",")),
                entity.getEstimatedSeconds(),
                entity.getReason(),
                entity.isAiGenerated(),
                CleaningPlan.Status.valueOf(entity.getStatus()),
                entity.getCreatedAt() != null ? entity.getCreatedAt() : Instant.now()
        );

        Set<UUID> knownRoomIds = roomRepository.findByHouseId(plan.houseId()).stream()
                .map(Room::getId)
                .collect(Collectors.toSet());
        planValidator.validate(plan, knownRoomIds);
        return plan;
    }

    private void markStatus(UUID planId, CleaningPlan.Status status) {
        planRepository.findById(planId).ifPresent(entity -> {
            entity.setStatus(status.name());
            planRepository.save(entity);
        });
    }
}
