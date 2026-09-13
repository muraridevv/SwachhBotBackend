package com.swachhbot.backend.robot.sensors.physical;

import com.swachhbot.backend.robot.model.SensorReading;
import com.swachhbot.backend.robot.sensors.Camera;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
public class PhysicalCamera implements Camera {
    private final String sensorId;

    @Override
    public SensorReading<byte[]> captureFrame() {
        log.debug("PhysicalCamera.captureFrame() - stub returning empty");
        return new SensorReading<>(Instant.now(), sensorId, new byte[0], 0.0);
    }

    @Override
    public SensorReading<String> getDetections() {
        log.debug("PhysicalCamera.getDetections() - stub returning empty");
        return new SensorReading<>(Instant.now(), sensorId, "[]", 0.0);
    }
}
