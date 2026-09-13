package com.swachhbot.backend.robot.slam;

import com.swachhbot.backend.robot.model.Orientation;
import com.swachhbot.backend.robot.model.RobotPosition;
import com.swachhbot.backend.robot.model.SensorReading;
import com.swachhbot.backend.robot.sensors.IMU;
import com.swachhbot.backend.robot.sensors.WheelEncoder;
import com.swachhbot.backend.robot.slam.model.Pose;
import lombok.extern.slf4j.Slf4j;

/**
 * Simple sensor fusion for Phase 17.
 * Uses IMU for precise orientation and wheel encoders for distance.
 */
@Slf4j
public class SimplePoseEstimator implements PoseEstimator {
    private Pose currentPose = Pose.origin();
    private WheelEncoder.Odometry lastOdometry = null;

    @Override
    public Pose update(SensorReading<WheelEncoder.Odometry> odometry, SensorReading<IMU.IMUData> imu) {
        if (lastOdometry == null) {
            lastOdometry = odometry.value();
            // First reading sets base orientation from IMU
            currentPose = new Pose(currentPose.position(), new Orientation(imu.value().orientationDeg()));
            return currentPose;
        }

        // 1. Calculate distance traveled since last tick
        double deltaDist = odometry.value().distanceTraveledMm() - lastOdometry.distanceTraveledMm();
        lastOdometry = odometry.value();

        // 2. Use IMU for heading (sensor fusion simplified: trust IMU for rotation)
        double heading = imu.value().orientationDeg();
        
        // 3. Update position
        double rad = Math.toRadians(heading - 90); // Adjusting to 0 deg = UP
        double dx = deltaDist * Math.cos(rad);
        double dy = deltaDist * Math.sin(rad);

        RobotPosition newPos = new RobotPosition(
            currentPose.position().x() + dx,
            currentPose.position().y() + dy
        );

        currentPose = new Pose(newPos, new Orientation(heading));
        return currentPose;
    }

    @Override
    public Pose getCurrentPose() {
        return currentPose;
    }
}
