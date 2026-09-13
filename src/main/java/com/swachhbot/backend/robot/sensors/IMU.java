package com.swachhbot.backend.robot.sensors;

import com.swachhbot.backend.robot.model.SensorReading;

/**
 * Inertial Measurement Unit (Accelerometer + Gyroscope).
 */
public interface IMU {
    record IMUData(
        double orientationDeg,    // 0-359
        double angularVelocity,   // deg/s
        double linearAcceleration // mm/s^2
    ) {}

    SensorReading<IMUData> read();
}
