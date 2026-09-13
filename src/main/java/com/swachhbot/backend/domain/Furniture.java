package com.swachhbot.backend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** A piece of furniture or a static obstacle inside a room. */
@Entity
@Table(name = "furniture")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Furniture {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    /** SOFA, BED, TABLE, CHAIR, REFRIGERATOR, ... */
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private double x;

    @Column(nullable = false)
    private double y;

    @Column(nullable = false)
    private double width;

    @Column(nullable = false)
    private double height;

    @Column(name = "rotation_deg", nullable = false)
    private double rotationDeg;
}