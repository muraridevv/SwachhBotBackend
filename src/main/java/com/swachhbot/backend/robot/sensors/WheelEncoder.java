package com.swachhbot.backend.robot.sensors;

import com.swachhbot.backend.robot.model.SensorReading;

/**
 * Odometry from wheel rotations.
 */
public interface WheelEncoder {
    record Odometry(
        double leftTicks,
        double rightTicks,
        double distanceTraveledMm
    ) {}

    SensorReading<Odometry> read();
}
