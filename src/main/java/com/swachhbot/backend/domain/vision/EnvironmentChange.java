package com.swachhbot.backend.domain.vision;

import com.swachhbot.backend.domain.House;
import com.swachhbot.backend.domain.RobotObject;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "environment_changes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvironmentChange {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "house_id", nullable = false)
    private House house;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "object_id", nullable = false)
    private RobotObject object;

    @Column(name = "change_type", nullable = false)
    private String changeType; // ADDED, MOVED, REMOVED, RELOCATED

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private double confidence;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;
}
