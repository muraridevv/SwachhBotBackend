package com.swachhbot.backend.domain.learning;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** A historical copy of a house's occupancy grid, used for change detection. */
@Entity
@Table(name = "map_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MapSnapshotEntity {

    @Id
    private UUID id;

    @Column(name = "house_id", nullable = false)
    private UUID houseId;

    @Column(name = "grid_width", nullable = false)
    private int gridWidth;

    @Column(name = "grid_height", nullable = false)
    private int gridHeight;

    @Column(name = "cell_size", nullable = false)
    private double cellSize;

    @Column(name = "map_data", nullable = false, columnDefinition = "text")
    private String mapData;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;
}
