package com.swachhbot.backend.domain.plan;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The <b>only</b> object the AI layer is allowed to hand to the robot.
 *
 * <p>This is a deliberately high-level, declarative description of intent —
 * "clean these rooms in this order with this many passes". It contains no
 * coordinates, velocities, motor commands or timings that could move hardware.
 * Turning a plan into motion is the job of the deterministic navigation engine.
 *
 * @param planId                 stable id (also persisted for audit)
 * @param houseId                house the plan applies to
 * @param naturalLanguage        the original human request (audit / explanation)
 * @param action                 what kind of sweeping operation is requested
 * @param rooms                  resolved rooms, in the order they should be visited
 * @param priority               global priority hint
 * @param passes                 number of cleaning passes (bounded by config)
 * @param excludedAreas          rooms the robot must not enter
 * @param estimatedDurationSeconds coarse estimate, for the user only
 * @param reason                 human-readable justification
 * @param aiGenerated            true if an LLM produced the draft
 * @param status                 lifecycle state
 * @param createdAt              creation timestamp
 */
public record CleaningPlan(
        UUID planId,
        UUID houseId,
        String naturalLanguage,
        Action action,
        List<PlannedRoom> rooms,
        Priority priority,
        int passes,
        List<String> excludedAreas,
        long estimatedDurationSeconds,
        String reason,
        boolean aiGenerated,
        Status status,
        Instant createdAt
) {

    /** High-level cleaning strategies the AI may select. */
    public enum Action {
        /** Clean exactly the listed rooms. */
        CLEAN,
        /** Clean every cleanable room in the house. */
        CLEAN_ALL,
        /** Clean everything except the excluded rooms. */
        CLEAN_EXCEPT
    }

    /** Scheduling hint; never a motor speed. */
    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }

    /** Plan lifecycle. Only VALIDATED plans may be executed. */
    public enum Status {
        DRAFT,
        VALIDATED,
        REJECTED,
        EXECUTING,
        COMPLETED
    }

    /**
     * A single room included in a plan.
     *
     * @param roomId        database id (null only for the rule-based fallback on unknown names)
     * @param name          human readable name
     * @param order         1-based visit order
     * @param priority      per-room priority (e.g. dirtiest first)
     * @param cleanRequired whether the robot should actually clean here
     * @param rationale     why this room was included (shown to the user)
     */
    public record PlannedRoom(
            UUID roomId,
            String name,
            int order,
            Priority priority,
            boolean cleanRequired,
            String rationale
    ) {
    }

    public CleaningPlan withStatus(Status newStatus) {
        return new CleaningPlan(
                planId, houseId, naturalLanguage, action, rooms, priority, passes,
                excludedAreas, estimatedDurationSeconds, reason, aiGenerated, newStatus, createdAt
        );
    }
}
