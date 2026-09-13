package com.swachhbot.backend.robot.slam.model;

import com.swachhbot.backend.robot.model.Orientation;
import com.swachhbot.backend.robot.model.RobotPosition;

/**
 * Robot pose in the World/Map coordinate frame.
 * Conventions:
 * - Units: millimeters (mm)
 * - X: East (right)
 * - Y: North (down in screen coords, but North in world)
 * - Orientation: Degrees (0-359), 0 is UP/North.
 */
public record Pose(RobotPosition position, Orientation orientation) {
    public static Pose origin() {
        return new Pose(new RobotPosition(0, 0), new Orientation(0));
    }
}
