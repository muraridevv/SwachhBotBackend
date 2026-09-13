package com.swachhbot.backend.domain.plan;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted audit record of a generated plan (validated or rejected).
 * Storing plans gives the AI searchable history of its own decisions and lets
 * the deterministic layer answer questions like "which rooms were not cleaned
 * in the last 3 days?".
 */
@Entity
@Table(name = "cleaning_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CleaningPlanEntity {

    @Id
    private UUID id;

    @Column(name = "house_id", nullable = false)
    private UUID houseId;

    @Column(name = "natural_language", columnDefinition = "text")
    private String naturalLanguage;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String priority;

    @Column(nullable = false)
    private int passes;

    @Column(name = "excluded_areas", columnDefinition = "text")
    private String excludedAreas;

    @Column(name = "rooms_json", nullable = false, columnDefinition = "text")
    private String roomsJson;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(name = "estimated_seconds", nullable = false)
    private long estimatedSeconds;

    @Column(nullable = false)
    private String status;

    @Column(name = "ai_generated", nullable = false)
    private boolean aiGenerated;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
