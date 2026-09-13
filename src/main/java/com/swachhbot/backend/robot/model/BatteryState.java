package com.swachhbot.backend.robot.model;

/** Level (0-100) and charging status. */
public record BatteryState(double level, boolean isCharging) {
}
