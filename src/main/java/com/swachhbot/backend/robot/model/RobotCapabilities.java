package com.swachhbot.backend.robot.model;

import java.util.Set;

public record RobotCapabilities(
        boolean hasMopping,
        boolean hasObjectDetection,
        boolean hasArCore,
        double maxLinearVelocity,
        double maxAngularVelocity,
        Set<String> supportedCommandTypes
) {
}
