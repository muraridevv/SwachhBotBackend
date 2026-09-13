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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ExplorationServiceTest {

    @Mock
    private Robot robot;

    @Mock
    private MapService mapService;

    @Mock
    private NavigationEngine navigationEngine;

    @Mock
    private FrontierDetector frontierDetector;

    @Mock
    private ExplorationGoalSelector goalSelector;

    @InjectMocks
    private ExplorationService service;

    private final UUID houseId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldStartExploration() {
        RobotPosition currentPos = new RobotPosition(50.0, 50.0);
        RobotState state = new RobotState("r1", houseId, currentPos, new Orientation(0), 0, new BatteryState(100, false), RobotStatus.IDLE, Instant.now());
        
        when(robot.getState()).thenReturn(state);
        when(mapService.getLatest(houseId)).thenReturn(new MapDto(
                UUID.randomUUID(), houseId, 10, 10, 100.0, "F".repeat(100), 1, Instant.now()));
        
        List<RobotPosition> frontiers = List.of(new RobotPosition(250.0, 50.0));
        when(frontierDetector.detectFrontiers(any())).thenReturn(frontiers);
        when(goalSelector.selectGoal(any(), any())).thenReturn(Optional.of(frontiers.get(0)));

        service.startExploration();

        assertThat(service.getStatus()).isEqualTo(ExplorationService.ExplorationStatus.EXPLORING);
        verify(navigationEngine).setGoal(frontiers.get(0));
    }

    @Test
    void shouldCompleteIfNoFrontiers() {
        RobotPosition currentPos = new RobotPosition(50.0, 50.0);
        RobotState state = new RobotState("r1", houseId, currentPos, new Orientation(0), 0, new BatteryState(100, false), RobotStatus.IDLE, Instant.now());

        when(robot.getState()).thenReturn(state);
        when(mapService.getLatest(houseId)).thenReturn(new MapDto(
                UUID.randomUUID(), houseId, 10, 10, 100.0, "F".repeat(100), 1, Instant.now()));
        when(frontierDetector.detectFrontiers(any())).thenReturn(Collections.emptyList());

        service.startExploration();

        assertThat(service.getStatus()).isEqualTo(ExplorationService.ExplorationStatus.COMPLETED);
    }
}
