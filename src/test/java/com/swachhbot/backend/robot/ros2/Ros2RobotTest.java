package com.swachhbot.backend.robot.ros2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swachhbot.backend.robot.model.RobotPosition;
import com.swachhbot.backend.robot.slam.SlamEngineFactory;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

public class Ros2RobotTest {

    @Test
    void shouldInitializeWithCorrectParams() {
        ObjectMapper mapper = new ObjectMapper();
        SlamEngineFactory factory = Mockito.mock(SlamEngineFactory.class);
        
        Ros2Robot robot = new Ros2Robot("swachhbot-01", "ws://localhost:9090", mapper, factory);
        
        assertThat(robot.getRobotId()).isEqualTo("swachhbot-01");
        assertThat(robot.getCapabilities().supportedCommandTypes()).contains("NAV2");
    }
}
