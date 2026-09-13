package com.swachhbot.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * A serialized occupancy grid for a house.
 *
 * <p>{@code mapData} is a compact string of cell characters:
 * U=UNKNOWN, F=FREE, O=OBSTACLE, C=CLEANED. Storing it as one row keeps
 * sync lightweight for the Android client; cell-level queries can be added later.
 */
@Entity
@Table(name = "occupancy_maps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OccupancyMap {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "house_id", nullable = false)
    private House house;

    @Column(name = "grid_width", nullable = false)
    private int gridWidth;

    @Column(name = "grid_height", nullable = false)
    private int gridHeight;

    @Column(name = "cell_size", nullable = false)
    private double cellSize;

    @Column(name = "map_data", nullable = false, columnDefinition = "text")
    private String mapData;

    @Column(nullable = false)
    private long version;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
