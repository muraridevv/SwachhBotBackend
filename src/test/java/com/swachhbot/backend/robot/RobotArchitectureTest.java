package com.swachhbot.backend.robot;

import com.swachhbot.backend.robot.model.MotionCommand;
import com.swachhbot.backend.service.CommandService;
import com.swachhbot.backend.service.RobotStateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class RobotArchitectureTest {

    @Autowired
    private Robot robot;

    @MockBean
    private RobotStateService stateService;

    @MockBean
    private CommandService commandService;

    @Test
    void shouldInjectSimulationRobotByDefault() {
        assertThat(robot).isInstanceOf(SimulationRobot.class);
        assertThat(robot.getRobotId()).isEqualTo("swachhbot-01");
    }

    @Test
    void shouldHandleMotionCommand() {
        MotionCommand cmd = new MotionCommand(1.0, 0.5, Duration.ofSeconds(2));
        robot.move(cmd);
    }

    @Test
    void shouldExecuteHighLevelCommand() {
        robot.executeCommand("STOP", null);
    }
}
