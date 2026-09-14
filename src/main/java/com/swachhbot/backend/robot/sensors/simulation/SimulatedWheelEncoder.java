package com.swachhbot.backend.robot.sensors.simulation;

import com.swachhbot.backend.robot.model.SensorReading;
import com.swachhbot.backend.robot.sensors.WheelEncoder;
import com.swachhbot.backend.service.RobotStateService;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
public class SimulatedWheelEncoder implements WheelEncoder {
    private final String robotId;
    private final RobotStateService stateService;
    private double lastX;
    private double lastY;
    private double totalDistance;

    @Override
    public SensorReading<Odometry> read() {
        var state = stateService.get(robotId);
        double distance = Math.hypot(state.x() - lastX, state.y() - lastY);
        totalDistance += distance;
        lastX = state.x();
        lastY = state.y();
        return new SensorReading<>(
            Instant.now(),
            robotId + "-encoder",
            new Odometry(totalDistance, totalDistance, totalDistance),
            1.0
        );
    }
}
