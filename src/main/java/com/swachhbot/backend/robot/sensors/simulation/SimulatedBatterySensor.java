package com.swachhbot.backend.robot.sensors.simulation;

import com.swachhbot.backend.dto.RobotDtos.RobotStateDto;
import com.swachhbot.backend.robot.model.BatteryState;
import com.swachhbot.backend.robot.model.SensorReading;
import com.swachhbot.backend.robot.sensors.BatterySensor;
import com.swachhbot.backend.service.RobotStateService;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
public class SimulatedBatterySensor implements BatterySensor {
    private final String robotId;
    private final RobotStateService stateService;

    @Override
    public SensorReading<BatteryState> read() {
        RobotStateDto state = stateService.get(robotId);
        return new SensorReading<>(
            Instant.now(), 
            "sim-battery-01", 
            new BatteryState(state.battery(), false), 
            1.0
        );
    }
}
