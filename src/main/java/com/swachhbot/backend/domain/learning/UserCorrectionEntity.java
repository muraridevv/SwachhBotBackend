package com.swachhbot.backend.domain.learning;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A human correction, e.g. "That chair is temporary." (no @Lob: Postgres text)
 * Corrections are the highest authority in the learning system and permanently
 * override derived assumptions.
 */
@Entity
@Table(name = "user_corrections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCorrectionEntity {

    @Id
    private UUID id;

    @Column(name = "house_id", nullable = false)
    private UUID houseId;

    @Column(name = "insight_id")
    private UUID insightId;

    /** OBJECT | ROOM | AREA */
    @Column(name = "target_type", nullable = false, length = 24)
    private String targetType;

    @Column(name = "target_key", nullable = false, length = 160)
    private String targetKey;

    @Column(nullable = false, columnDefinition = "text")
    private String assertion;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;
}
