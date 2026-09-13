package com.swachhbot.backend.robot.sensors.physical;

import com.swachhbot.backend.robot.model.SensorReading;
import com.swachhbot.backend.robot.sensors.DistanceSensor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class PhysicalDistanceSensor implements DistanceSensor {
    private final String sensorId;

    @Override
    public SensorReading<Map<String, Double>> read() {
        log.debug("PhysicalDistanceSensor.read() - stub returning empty");
        return new SensorReading<>(Instant.now(), sensorId, Collections.emptyMap(), 0.0);
    }
}
