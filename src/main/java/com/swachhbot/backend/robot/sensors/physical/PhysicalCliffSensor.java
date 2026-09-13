package com.swachhbot.backend.robot.sensors.physical;

import com.swachhbot.backend.robot.model.SensorReading;
import com.swachhbot.backend.robot.sensors.CliffSensor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
public class PhysicalCliffSensor implements CliffSensor {
    private final String sensorId;

    @Override
    public SensorReading<Boolean> isTriggered() {
        log.debug("PhysicalCliffSensor.isTriggered() - stub");
        return new SensorReading<>(Instant.now(), sensorId, false, 1.0);
    }
}
