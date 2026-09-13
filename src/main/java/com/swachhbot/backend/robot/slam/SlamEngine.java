package com.swachhbot.backend.robot.slam;

import com.swachhbot.backend.robot.model.SensorReading;
import com.swachhbot.backend.robot.navigation.model.OccupancyGrid;
import com.swachhbot.backend.robot.sensors.IMU;
import com.swachhbot.backend.robot.sensors.WheelEncoder;
import com.swachhbot.backend.robot.slam.model.Pose;

import java.util.Map;

/**
 * Top-level SLAM engine orchestrating Localization and Mapping.
 */
public interface SlamEngine {
    /** Processes one tick of sensor data. */
    void process(
        SensorReading<WheelEncoder.Odometry> odometry,
        SensorReading<IMU.IMUData> imu,
        SensorReading<Map<String, Double>> distanceData
    );

    Pose getEstimatedPose();

    OccupancyGrid getMap();
}
