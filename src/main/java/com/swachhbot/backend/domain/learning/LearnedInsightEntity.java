package com.swachhbot.backend.domain.learning;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A single piece of knowledge the robot has learned about a house.
 *
 * <p>Identity is {@code (houseId, category, subjectKey)} so repeated observations
 * reinforce one row instead of creating duplicates. {@code confidence} grows with
 * evidence and can be overridden by a human correction.
 */
@Entity
@Table(
        name = "learned_insights",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_insight_identity",
                columnNames = {"house_id", "category", "subject_key"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearnedInsightEntity {

    @Id
    private UUID id;

    @Column(name = "house_id", nullable = false)
    private UUID houseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InsightCategory category;

    /** Stable machine key, e.g. {@code ROOM:KITCHEN} or {@code AREA:300:100}. */
    @Column(name = "subject_key", nullable = false, length = 160)
    private String subjectKey;

    /** Human label, e.g. {@code Kitchen}. */
    @Column(name = "subject_label", nullable = false)
    private String subjectLabel;

    @Column(nullable = false, length = 512)
    private String summary;

    @Column(nullable = false)
    private double confidence;

    @Column(name = "evidence_count", nullable = false)
    private int evidenceCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InsightStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InsightSource source;

    /** Free-form JSON with category specific extras (e.g. learned passes). */
    @Column(columnDefinition = "text")
    private String details;

    @Column(name = "first_learned_at", nullable = false)
    private Instant firstLearnedAt;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;
}
