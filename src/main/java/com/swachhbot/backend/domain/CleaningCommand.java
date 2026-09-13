package com.swachhbot.backend.domain;

import com.swachhbot.backend.domain.enums.CommandStatus;
import com.swachhbot.backend.domain.enums.CommandType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** A command dispatched to a robot, tracked until acknowledged. */
@Entity
@Table(name = "cleaning_commands")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CleaningCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "robot_id", nullable = false)
    private String robotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "house_id")
    private House house;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommandType command;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommandStatus status;

    /** Free-form JSON payload (e.g. target coordinates for GO_TO). */
    @Column(columnDefinition = "text")
    private String payload;

    @CreationTimestamp
    @Column(name = "issued_at", updatable = false)
    private Instant issuedAt;

    @Column(name = "acked_at")
    private Instant ackedAt;
}
