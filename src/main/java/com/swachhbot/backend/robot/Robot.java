package com.swachhbot.backend.robot;

import com.swachhbot.backend.robot.model.*;
import com.swachhbot.backend.robot.sensors.*;
import com.swachhbot.backend.robot.navigation.model.OccupancyGrid;

import java.util.UUID;

/**
 * Hardware-independent abstraction of a SwachhBot robot.
 */
public interface Robot {

    String getRobotId();

    RobotState getState();

    RobotPosition getPosition();

    Orientation getOrientation();

    BatteryState getBattery();

    RobotCapabilities getCapabilities();

    CleaningState getCleaningState();

    MotionState getMotionState();

    // ----- Sensors (Phase 13) -----

    DistanceSensor getDistanceSensor();

    Camera getCamera();

    IMU getImu();

    WheelEncoder getWheelEncoder();

    BatterySensor getBatterySensor();

    CliffSensor getCliffSensor();

    // ----- SLAM (Phase 17) -----

    RobotPosition getEstimatedPosition();

    Orientation getEstimatedOrientation();

    OccupancyGrid getSlamMap();

    void updateSlam();

    /** Move the robot using velocities. */
    void move(MotionCommand command);

    /** Abrupt stop. */
    void stop();

    /** Pause current operation (preserves plan). */
    void pause();

    /** Resume current operation. */
    void resume();

    /** High-level command execution (Phase 9/11 compatibility). */
    RobotCommandResult executeCommand(String type, String payload);
}
