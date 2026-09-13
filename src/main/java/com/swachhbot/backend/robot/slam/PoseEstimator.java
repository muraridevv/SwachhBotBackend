package com.swachhbot.backend.robot.slam;

import com.swachhbot.backend.robot.model.SensorReading;
import com.swachhbot.backend.robot.sensors.IMU;
import com.swachhbot.backend.robot.sensors.WheelEncoder;
import com.swachhbot.backend.robot.slam.model.Pose;

/**
 * Interface for estimating robot pose using sensor fusion (Odometry + IMU).
 */
public interface PoseEstimator {
    /** 
     * Updates the pose estimate based on new sensor data.
     * @param odometry Latest wheel odometry
     * @param imu Latest IMU data
     * @return The updated Pose estimate
     */
    Pose update(SensorReading<WheelEncoder.Odometry> odometry, SensorReading<IMU.IMUData> imu);

    Pose getCurrentPose();
}
