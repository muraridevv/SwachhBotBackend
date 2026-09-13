package com.swachhbot.backend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** A completed (or in-progress) cleaning run. */
@Entity
@Table(name = "cleaning_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CleaningSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "house_id", nullable = false)
    private House house;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "duration_seconds", nullable = false)
    private long durationSeconds;

    @Column(name = "cleaned_percentage", nullable = false)
    private double cleanedPercentage;

    @Column(name = "area_cleaned_sqm", nullable = false)
    private double areaCleanedSqm;
}
