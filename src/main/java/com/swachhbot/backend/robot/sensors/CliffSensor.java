package com.swachhbot.backend.robot.sensors;

import com.swachhbot.backend.robot.model.SensorReading;

public interface CliffSensor {
    /** Returns true if a cliff is detected. */
    SensorReading<Boolean> isTriggered();
}
