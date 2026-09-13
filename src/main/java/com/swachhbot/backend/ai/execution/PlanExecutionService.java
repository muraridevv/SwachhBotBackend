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
import com.swachhbot.backend.robot.Robot;
import com.swachhbot.backend.robot.model.RobotCommandResult;
import com.swachhbot.backend.robot.navigation.NavigationEngine;
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

    private final Robot robot;
    private final NavigationEngine navigationEngine;
    private final CleaningPlanRepository planRepository;
    private final RoomRepository roomRepository;
    private final PlanValidator planValidator;
    private final ObjectMapper objectMapper;

    @Transactional
    public RobotCommandResult execute(UUID planId, String robotId) {
        CleaningPlan plan = loadValidated(planId);

        // START NAVIGATION ENGINE (Phase 14)
        navigationEngine.startCleaning(plan);

        markStatus(planId, CleaningPlan.Status.EXECUTING);
        log.info("Dispatched plan {} to navigation engine for robot {}", planId, robotId);
        
        return new RobotCommandResult(UUID.randomUUID(), "EXECUTING", Instant.now());
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
