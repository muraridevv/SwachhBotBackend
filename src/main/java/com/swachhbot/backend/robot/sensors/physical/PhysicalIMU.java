package com.swachhbot.backend.robot.sensors.physical;

import com.swachhbot.backend.robot.model.SensorReading;
import com.swachhbot.backend.robot.sensors.IMU;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
public class PhysicalIMU implements IMU {
    private final String sensorId;

    @Override
    public SensorReading<IMUData> read() {
        log.debug("PhysicalIMU.read() - stub");
        return new SensorReading<>(Instant.now(), sensorId, new IMUData(0, 0, 0), 0.0);
    }
}
