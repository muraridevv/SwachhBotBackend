package com.swachhbot.backend.domain.intelligence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "room_cleaning_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomCleaningStats {

    @Id
    @Column(name = "room_id")
    private UUID roomId;

    @Column(name = "last_cleaned_at")
    private Instant lastCleanedAt;

    @Column(name = "total_cleaned_count", nullable = false)
    private int totalCleanedCount;

    @Column(name = "avg_coverage", nullable = false)
    private double avgCoverage;

    @Column(name = "avg_duration_sec", nullable = false)
    private long avgDurationSec;

    @Column(name = "dirt_score", nullable = false)
    private double dirtScore;
}
