package com.swachhbot.backend.robot.sensors.simulation;

import com.swachhbot.backend.robot.model.SensorReading;
import com.swachhbot.backend.robot.sensors.WheelEncoder;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
public class SimulatedWheelEncoder implements WheelEncoder {
    private final String robotId;

    @Override
    public SensorReading<Odometry> read() {
        // Mock odometry for now
        return new SensorReading<>(
            Instant.now(),
            "sim-encoder-01",
            new Odometry(1000, 1000, 50.0),
            1.0
        );
    }
}
