package com.swachhbot.backend.robot.slam;

import com.swachhbot.backend.robot.model.SensorReading;
import com.swachhbot.backend.robot.navigation.model.OccupancyGrid;
import com.swachhbot.backend.robot.sensors.IMU;
import com.swachhbot.backend.robot.sensors.WheelEncoder;
import com.swachhbot.backend.robot.slam.model.Pose;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Standard implementation of the SLAM engine for Phase 17.
 */
@Slf4j
@RequiredArgsConstructor
public class SimpleSlamEngine implements SlamEngine {

    private final PoseEstimator poseEstimator = new SimplePoseEstimator();
    private final MapBuilder mapBuilder = new SimpleMapBuilder();
    private final OccupancyGrid grid;

    @Override
    public void process(
        SensorReading<WheelEncoder.Odometry> odometry,
        SensorReading<IMU.IMUData> imu,
        SensorReading<Map<String, Double>> distanceData
    ) {
        // 1. Localization: Update pose estimate
        Pose estimatedPose = poseEstimator.update(odometry, imu);

        // 2. Mapping: Update grid from observations
        mapBuilder.updateMap(grid, estimatedPose, distanceData);
        
        log.debug("SLAM updated: pose={}", estimatedPose);
    }

    @Override
    public Pose getEstimatedPose() {
        return poseEstimator.getCurrentPose();
    }

    @Override
    public OccupancyGrid getMap() {
        return grid;
    }
}
