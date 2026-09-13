package com.swachhbot.backend.robot.sensors.simulation;

import com.swachhbot.backend.dto.RobotDtos.RobotStateDto;
import com.swachhbot.backend.robot.model.SensorReading;
import com.swachhbot.backend.robot.sensors.IMU;
import com.swachhbot.backend.service.RobotStateService;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
public class SimulatedIMU implements IMU {
    private final String robotId;
    private final RobotStateService stateService;

    @Override
    public SensorReading<IMUData> read() {
        RobotStateDto state = stateService.get(robotId);
        return new SensorReading<>(
            Instant.now(),
            "sim-imu-01",
            new IMUData(state.rotation(), 0.0, 0.0),
            1.0
        );
    }
}
