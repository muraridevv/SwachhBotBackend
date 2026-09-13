package com.swachhbot.backend.domain;

import com.swachhbot.backend.domain.enums.ObjectCategory;
import com.swachhbot.backend.domain.enums.ObjectStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A persistently remembered object detected by the robot's camera.
 *
 * <p>Tracks lifecycle metadata (first/last seen, detection count, status) so the
 * backend can distinguish permanent furniture from temporary litter.
 */
@Entity
@Table(name = "robot_objects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RobotObject {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "house_id", nullable = false)
    private House house;

    /** e.g. "sofa", "bottle", "person". */
    @Column(nullable = false)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ObjectCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ObjectStatus status;

    @Column(nullable = false)
    private double x;

    @Column(nullable = false)
    private double y;

    @Column(nullable = false)
    private double confidence;

    @Column(name = "first_detected", nullable = false)
    private Instant firstDetected;

    @Column(name = "last_detected", nullable = false)
    private Instant lastDetected;

    @Column(name = "detection_count", nullable = false)
    private int detectionCount;
}
