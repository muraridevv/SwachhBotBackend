package com.swachhbot.backend.robot.sensors;

import com.swachhbot.backend.robot.model.BatteryState;
import com.swachhbot.backend.robot.model.SensorReading;

public interface BatterySensor {
    SensorReading<BatteryState> read();
}
