package com.swachhbot.backend.robot.navigation;

import com.swachhbot.backend.robot.model.RobotPosition;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Deterministically selects the best exploration goal from a list of frontiers.
 */
@Component
public class ExplorationGoalSelector {

    public Optional<RobotPosition> selectGoal(RobotPosition currentPos, List<RobotPosition> frontiers) {
        // Nearest frontier heuristic
        return frontiers.stream()
                .min(Comparator.comparingDouble(f -> distance(currentPos, f)));
    }

    private double distance(RobotPosition a, RobotPosition b) {
        return Math.sqrt(Math.pow(a.x() - b.x(), 2) + Math.pow(a.y() - b.y(), 2));
    }
}
