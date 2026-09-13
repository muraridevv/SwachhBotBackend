package com.swachhbot.backend.robot.navigation.model;

import com.swachhbot.backend.robot.model.RobotPosition;
import java.util.List;

public record Path(List<RobotPosition> points) {
    public boolean isEmpty() {
        return points == null || points.isEmpty();
    }
}
