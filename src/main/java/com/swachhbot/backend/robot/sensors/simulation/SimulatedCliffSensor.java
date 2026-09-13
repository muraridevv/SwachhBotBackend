package com.swachhbot.backend.robot.sensors.simulation;

import com.swachhbot.backend.robot.model.SensorReading;
import com.swachhbot.backend.robot.sensors.CliffSensor;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
public class SimulatedCliffSensor implements CliffSensor {
    private final String robotId;

    @Override
    public SensorReading<Boolean> isTriggered() {
        return new SensorReading<>(Instant.now(), "sim-cliff-01", false, 1.0);
    }
}
