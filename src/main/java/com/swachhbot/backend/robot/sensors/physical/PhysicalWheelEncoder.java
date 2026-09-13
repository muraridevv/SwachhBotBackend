package com.swachhbot.backend.robot.sensors.physical;

import com.swachhbot.backend.robot.model.SensorReading;
import com.swachhbot.backend.robot.sensors.WheelEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
public class PhysicalWheelEncoder implements WheelEncoder {
    private final String sensorId;

    @Override
    public SensorReading<Odometry> read() {
        log.debug("PhysicalWheelEncoder.read() - stub");
        return new SensorReading<>(Instant.now(), sensorId, new Odometry(0, 0, 0), 0.0);
    }
}
