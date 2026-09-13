package com.swachhbot.backend.robot.navigation;

import com.swachhbot.backend.domain.enums.RobotStatus;
import com.swachhbot.backend.robot.Robot;
import com.swachhbot.backend.robot.model.RobotPosition;
import com.swachhbot.backend.robot.model.RobotState;
import com.swachhbot.backend.robot.navigation.model.OccupancyGrid;
import com.swachhbot.backend.service.MapService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates Autonomous Exploration (Phase 16).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExplorationService {

    public enum ExplorationStatus {
        IDLE,
        EXPLORING,
        COMPLETED,
        BLOCKED,
        ERROR
    }

    private final Robot robot;
    private final MapService mapService;
    private final NavigationEngine navigationEngine;
    private final FrontierDetector frontierDetector;
    private final ExplorationGoalSelector goalSelector;

    @Getter
    private ExplorationStatus status = ExplorationStatus.IDLE;

    public void startExploration() {
        log.info("Starting autonomous exploration");
        status = ExplorationStatus.EXPLORING;
        findAndSetNextGoal();
    }

    public void stopExploration() {
        log.info("Stopping exploration");
        status = ExplorationStatus.IDLE;
        navigationEngine.stop();
    }

    @Scheduled(fixedRate = 1000)
    public void monitorExploration() {
        if (status != ExplorationStatus.EXPLORING) return;

        RobotState state = robot.getState();
        if (state.status() == RobotStatus.ERROR) {
            status = ExplorationStatus.ERROR;
            return;
        }

        // If the robot is idle (reached goal or stopped), find next frontier
        if (state.status() == RobotStatus.IDLE) {
            findAndSetNextGoal();
        }
    }

    private void findAndSetNextGoal() {
        RobotState state = robot.getState();
        if (state.houseId() == null) {
            log.warn("Exploration failed: No houseId associated with robot");
            status = ExplorationStatus.ERROR;
            return;
        }

        try {
            var mapDto = mapService.getLatest(state.houseId());
            OccupancyGrid grid = new OccupancyGrid(
                    mapDto.gridWidth(), mapDto.gridHeight(), mapDto.cellSize(), mapDto.mapData());

            List<RobotPosition> frontiers = frontierDetector.detectFrontiers(grid);
            
            if (frontiers.isEmpty()) {
                log.info("No more frontiers found. Exploration COMPLETED.");
                status = ExplorationStatus.COMPLETED;
                navigationEngine.stop();
                return;
            }

            goalSelector.selectGoal(state.position(), frontiers).ifPresentOrElse(
                goal -> {
                    log.info("Navigating to next frontier: {}", goal);
                    navigationEngine.setGoal(goal);
                },
                () -> {
                    log.warn("Could not select a goal from frontiers. COMPLETED.");
                    status = ExplorationStatus.COMPLETED;
                }
            );

        } catch (Exception e) {
            log.error("Exploration goal finding failed: {}", e.getMessage());
            status = ExplorationStatus.ERROR;
        }
    }
}
