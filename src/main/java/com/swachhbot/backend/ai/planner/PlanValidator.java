package com.swachhbot.backend.ai.planner;

import com.swachhbot.backend.ai.config.AiProperties;
import com.swachhbot.backend.domain.plan.CleaningPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The gate every plan must pass before it can be executed.
 *
 * <p>This is the second half of the safety boundary: even if a model produced a
 * well-formed {@link CleaningPlan}, it is rejected here unless it is bounded,
 * self-consistent and references only rooms that actually exist in the house.
 * Nothing downstream trusts the LLM.
 */
@Component
@RequiredArgsConstructor
public class PlanValidator {

    private final AiProperties properties;

    /**
     * @param plan        the candidate plan
     * @param knownRoomIds rooms that legitimately exist in the target house
     * @throws PlanValidationException if any rule fails
     */
    public void validate(CleaningPlan plan, Set<UUID> knownRoomIds) {
        if (plan == null) {
            throw new PlanValidationException("Plan is null");
        }
        if (plan.houseId() == null) {
            throw new PlanValidationException("Plan has no house id");
        }
        if (plan.action() == null) {
            throw new PlanValidationException("Plan has no action");
        }
        if (plan.priority() == null) {
            throw new PlanValidationException("Plan has no priority");
        }
        if (plan.reason() == null || plan.reason().isBlank()) {
            throw new PlanValidationException("Plan must explain itself (empty reason)");
        }

        // -- Bounds ------------------------------------------------------------
        if (plan.passes() < 1 || plan.passes() > properties.getMaxPasses()) {
            throw new PlanValidationException(
                    "Passes must be between 1 and " + properties.getMaxPasses() + " (got " + plan.passes() + ")");
        }
        if (plan.rooms().isEmpty()) {
            throw new PlanValidationException("Plan contains no rooms to clean");
        }
        if (plan.rooms().size() > properties.getMaxPlannedRooms()) {
            throw new PlanValidationException(
                    "Plan exceeds the maximum of " + properties.getMaxPlannedRooms() + " rooms");
        }
        if (plan.estimatedDurationSeconds() <= 0
                || plan.estimatedDurationSeconds() > properties.getMaxDurationSeconds()) {
            throw new PlanValidationException(
                    "Estimated duration must be between 1 and " + properties.getMaxDurationSeconds() + " seconds");
        }

        // -- Referential integrity --------------------------------------------
        Set<UUID> seen = new HashSet<>();
        for (CleaningPlan.PlannedRoom room : plan.rooms()) {
            if (room.roomId() != null && !knownRoomIds.contains(room.roomId())) {
                throw new PlanValidationException("Plan references an unknown room: " + room.name());
            }
            if (!seen.add(room.roomId())) {
                throw new PlanValidationException("Plan contains a duplicate room: " + room.name());
            }
        }

        // -- Exclusion consistency --------------------------------------------
        for (String excluded : plan.excludedAreas()) {
            boolean stillPlanned = plan.rooms().stream()
                    .anyMatch(r -> r.cleanRequired() && r.name().equalsIgnoreCase(excluded));
            if (stillPlanned) {
                throw new PlanValidationException(
                        "Room '" + excluded + "' is both excluded and scheduled for cleaning");
            }
        }

        // -- Execution sanity --------------------------------------------------
        boolean anyToClean = plan.rooms().stream().anyMatch(CleaningPlan.PlannedRoom::cleanRequired);
        if (!anyToClean) {
            throw new PlanValidationException("Plan would not clean any room");
        }
    }
}
