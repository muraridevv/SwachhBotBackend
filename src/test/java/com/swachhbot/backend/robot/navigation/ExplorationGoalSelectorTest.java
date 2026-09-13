package com.swachhbot.backend.robot.navigation;

import com.swachhbot.backend.robot.model.RobotPosition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class ExplorationGoalSelectorTest {

    @Test
    void shouldSelectNearestFrontier() {
        ExplorationGoalSelector selector = new ExplorationGoalSelector();
        RobotPosition currentPos = new RobotPosition(100.0, 100.0);
        
        RobotPosition near = new RobotPosition(110.0, 110.0);
        RobotPosition far = new RobotPosition(500.0, 500.0);
        
        Optional<RobotPosition> goal = selector.selectGoal(currentPos, List.of(far, near));
        
        assertThat(goal).isPresent();
        assertThat(goal.get()).isEqualTo(near);
    }

    @Test
    void shouldReturnEmptyIfNoFrontiers() {
        ExplorationGoalSelector selector = new ExplorationGoalSelector();
        Optional<RobotPosition> goal = selector.selectGoal(new RobotPosition(0, 0), List.of());
        assertThat(goal).isEmpty();
    }
}
