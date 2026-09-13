package com.swachhbot.backend.domain;

import com.swachhbot.backend.domain.enums.RobotStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/** Latest known live state of a single robot (keyed by robot_id). */
@Entity
@Table(name = "robot_state")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RobotStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "robot_id", nullable = false, unique = true)
    private String robotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "house_id")
    private House house;

    @Column(nullable = false)
    private double x;

    @Column(nullable = false)
    private double y;

    @Column(nullable = false)
    private double rotation;

    @Column(nullable = false)
    private double velocity;

    @Column(nullable = false)
    private double battery;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RobotStatus status;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
