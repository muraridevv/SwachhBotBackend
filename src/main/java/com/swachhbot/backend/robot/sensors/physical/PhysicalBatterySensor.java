package com.swachhbot.backend.robot.sensors.physical;

import com.swachhbot.backend.robot.model.BatteryState;
import com.swachhbot.backend.robot.model.SensorReading;
import com.swachhbot.backend.robot.sensors.BatterySensor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
public class PhysicalBatterySensor implements BatterySensor {
    private final String sensorId;

    @Override
    public SensorReading<BatteryState> read() {
        log.debug("PhysicalBatterySensor.read() - stub");
        return new SensorReading<>(Instant.now(), sensorId, new BatteryState(100.0, false), 1.0);
    }
}
