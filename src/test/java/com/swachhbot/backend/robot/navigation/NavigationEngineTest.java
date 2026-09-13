package com.swachhbot.backend.robot.navigation;

import com.swachhbot.backend.domain.enums.RobotStatus;
import com.swachhbot.backend.dto.MapDtos.MapDto;
import com.swachhbot.backend.robot.Robot;
import com.swachhbot.backend.robot.model.*;
import com.swachhbot.backend.service.MapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class NavigationEngineTest {

    @Mock
    private Robot robot;

    @Mock
    private MapService mapService;

    @InjectMocks
    private NavigationEngine engine;

    private final UUID houseId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(robot.getCapabilities()).thenReturn(new com.swachhbot.backend.robot.model.RobotCapabilities(
            true, true, true, 500.0, 180.0, Collections.emptySet()
        ));
    }

    @Test
    void shouldPlanAndDriveToGoal() {
        RobotPosition start = new RobotPosition(50.0, 50.0);
        RobotPosition goal = new RobotPosition(250.0, 50.0);
        RobotState state = new RobotState("r1", houseId, start, new Orientation(0), 0, new BatteryState(100, false), RobotStatus.IDLE, Instant.now());
        
        when(robot.getState()).thenReturn(state);
        when(robot.getEstimatedPosition()).thenReturn(start);
        when(robot.getEstimatedOrientation()).thenReturn(new Orientation(0));
        when(robot.getMotionState()).thenReturn(new MotionState(0, 0, 0, false));
        when(mapService.getLatest(houseId)).thenReturn(new MapDto(
                UUID.randomUUID(), houseId, 10, 10, 100.0, "FFFFFFFFFF".repeat(10), 1, Instant.now()));

        engine.setGoal(goal);
        engine.controlLoop(); // Plans path, reaches start point (dist=0), removes first point
        engine.controlLoop(); // Calculates move to next point

        verify(robot, atLeastOnce()).move(any(MotionCommand.class));
    }

    @Test
    void shouldStopAndReplanOnObstacle() {
        RobotPosition start = new RobotPosition(50.0, 50.0);
        RobotPosition goal = new RobotPosition(250.0, 50.0);
        RobotState state = new RobotState("r1", houseId, start, new Orientation(0), 0, new BatteryState(100, false), RobotStatus.CLEANING, Instant.now());

        when(robot.getState()).thenReturn(state);
        when(robot.getEstimatedPosition()).thenReturn(start);
        when(robot.getEstimatedOrientation()).thenReturn(new Orientation(0));
        
        // First call: no collision. Second call: colliding.
        when(robot.getMotionState())
                .thenReturn(new MotionState(0, 0, 0, false))
                .thenReturn(new MotionState(0, 0, 0, true));
        
        when(mapService.getLatest(houseId)).thenReturn(new MapDto(
                UUID.randomUUID(), houseId, 10, 10, 100.0, "FFFFFFFFFF".repeat(10), 1, Instant.now()));

        engine.setGoal(goal);
        engine.controlLoop(); // Plans path
        engine.controlLoop(); // Detects collision, stops and replans

        verify(robot).stop();
    }
}
