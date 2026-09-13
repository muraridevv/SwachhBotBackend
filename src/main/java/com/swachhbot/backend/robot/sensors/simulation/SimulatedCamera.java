package com.swachhbot.backend.robot.sensors.simulation;

import com.swachhbot.backend.robot.model.SensorReading;
import com.swachhbot.backend.robot.sensors.Camera;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
public class SimulatedCamera implements Camera {
    private final String robotId;

    @Override
    public SensorReading<byte[]> captureFrame() {
        return new SensorReading<>(Instant.now(), "sim-camera-01", new byte[0], 1.0);
    }

    @Override
    public SensorReading<String> getDetections() {
        return new SensorReading<>(Instant.now(), "sim-camera-01", "[]", 1.0);
    }
}
