package com.swachhbot.backend.robot.sensors.simulation;

import com.swachhbot.backend.dto.RobotDtos.RobotStateDto;
import com.swachhbot.backend.robot.model.SensorReading;
import com.swachhbot.backend.robot.sensors.DistanceSensor;
import com.swachhbot.backend.service.RobotStateService;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class SimulatedDistanceSensor implements DistanceSensor {
    private final String robotId;
    private final RobotStateService stateService;

    @Override
    public SensorReading<Map<String, Double>> read() {
        // In simulation, we pull the "ground truth" or pre-calculated readings from the state service.
        // For Phase 13, we mock some directional data if real LiDAR data isn't in the DTO yet.
        Map<String, Double> readings = new HashMap<>();
        readings.put("front", 500.0);
        readings.put("left", 1200.0);
        readings.put("right", 1200.0);
        readings.put("rear", 2000.0);

        return new SensorReading<>(Instant.now(), "sim-lidar-01", readings, 1.0);
    }
}
