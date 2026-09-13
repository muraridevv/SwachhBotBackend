package com.swachhbot.backend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A location the robot repeatedly struggles with
 * (e.g. "gets stuck near sofa"). Frequency accumulates across sessions.
 */
@Entity
@Table(name = "problem_areas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemArea {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "house_id", nullable = false)
    private House house;

    @Column(nullable = false)
    private double x;

    @Column(nullable = false)
    private double y;

    @Column(nullable = false)
    private double radius;

    @Column(nullable = false, length = 512)
    private String description;

    @Column(nullable = false)
    private int frequency;

    @Column(name = "last_seen", nullable = false)
    private Instant lastSeen;
}
