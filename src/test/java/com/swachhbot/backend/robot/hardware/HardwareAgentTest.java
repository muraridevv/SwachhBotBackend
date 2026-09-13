package com.swachhbot.backend.robot.hardware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swachhbot.backend.dto.RobotDtos.RobotCommandMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

public class HardwareAgentTest {

    @Mock
    private MotorController motorController;
    
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private HardwareAgent agent;
    private final String robotId = "swachhbot-01";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        agent = new HardwareAgent(robotId, motorController, objectMapper);
    }

    @Test
    void shouldDispatchMoveToMotorController() {
        String payload = "{\"linear\": 200.0, \"angular\": 0.0, \"duration\": 100}";
        RobotCommandMessage msg = new RobotCommandMessage(
            RobotCommandMessage.TYPE, robotId, "MOVE", payload, Instant.now());
        
        try {
            agent.handleIncomingMessage(objectMapper.writeValueAsString(msg));
            verify(motorController).setVelocity(200.0, 0.0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldStopOnStopCommand() {
        RobotCommandMessage msg = new RobotCommandMessage(
            RobotCommandMessage.TYPE, robotId, "STOP", null, Instant.now());
        
        try {
            agent.handleIncomingMessage(objectMapper.writeValueAsString(msg));
            verify(motorController).stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldIgnoreCommandsForOtherRobots() {
        RobotCommandMessage msg = new RobotCommandMessage(
            RobotCommandMessage.TYPE, "other-robot", "STOP", null, Instant.now());
        
        try {
            agent.handleIncomingMessage(objectMapper.writeValueAsString(msg));
            verify(motorController, never()).stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
