package com.swachhbot.backend.domain.assistant;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A robot action the assistant has proposed but <b>not</b> executed.
 *
 * <p>This table is the safety boundary of Phase 11. The LLM can only ever create
 * a {@code PENDING} row. Turning it into real movement requires an explicit,
 * authenticated confirmation that routes through the same deterministic
 * execution path used by the rest of the system.
 */
@Entity
@Table(name = "assistant_actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssistantActionEntity {

    @Id
    private UUID id;

    @Column(name = "house_id", nullable = false)
    private UUID houseId;

    @Column(name = "robot_id", nullable = false)
    private String robotId;

    @Column(name = "conversation_id")
    private String conversationId;

    /** START_CLEANING | PAUSE_CLEANING | STOP_CLEANING */
    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssistantActionStatus status;

    @Column(nullable = false, length = 512)
    private String summary;

    /** Pre-validated payload built by the server (never raw model text). */
    @Column(columnDefinition = "text")
    private String payload;

    @Column(name = "plan_id")
    private UUID planId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
