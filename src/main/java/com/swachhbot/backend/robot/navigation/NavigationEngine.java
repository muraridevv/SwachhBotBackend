package com.swachhbot.backend.robot.navigation;

import com.swachhbot.backend.domain.Room;
import com.swachhbot.backend.domain.enums.RobotStatus;
import com.swachhbot.backend.domain.plan.CleaningPlan;
import com.swachhbot.backend.repository.RoomRepository;
import com.swachhbot.backend.robot.Robot;
import com.swachhbot.backend.robot.model.MotionCommand;
import com.swachhbot.backend.robot.model.Orientation;
import com.swachhbot.backend.robot.model.RobotPosition;
import com.swachhbot.backend.robot.model.RobotState;
import com.swachhbot.backend.robot.navigation.model.OccupancyGrid;
import com.swachhbot.backend.robot.navigation.model.Path;
import com.swachhbot.backend.service.MapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Deterministic Navigation Engine (Phase 14).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NavigationEngine {

    private final Robot robot;
    private final MapService mapService;
    private final RoomRepository roomRepository;
    private final PathPlanner pathPlanner = new AStarPathPlanner();
    private final MotionController motionController = new MotionController();

    private final Queue<RobotPosition> goalQueue = new ConcurrentLinkedQueue<>();
    private final AtomicReference<RobotPosition> currentGoal = new AtomicReference<>();
    private final AtomicReference<List<RobotPosition>> currentPath = new AtomicReference<>();

    public void setGoal(RobotPosition goal) {
        log.info("Setting navigation goal: {}", goal);
        goalQueue.clear();
        currentGoal.set(goal);
        replanningRequired();
    }

    public void startCleaning(CleaningPlan plan) {
        log.info("Starting navigation for cleaning plan: {}", plan.planId());
        goalQueue.clear();
        
        List<Room> rooms = roomRepository.findByHouseId(plan.houseId());
        Map<UUID, Room> roomMap = new HashMap<>();
        rooms.forEach(r -> roomMap.put(r.getId(), r));

        plan.rooms().stream()
                .filter(CleaningPlan.PlannedRoom::cleanRequired)
                .sorted(Comparator.comparingInt(CleaningPlan.PlannedRoom::order))
                .forEach(pr -> {
                    Room room = roomMap.get(pr.roomId());
                    if (room != null) {
                        // For Phase 14, we just navigate to the center of the room.
                        goalQueue.add(new RobotPosition(room.getX() + room.getWidth() / 2, room.getY() + room.getHeight() / 2));
                    }
                });
        
        advanceGoal();
    }

    public void stop() {
        log.info("Stopping navigation");
        goalQueue.clear();
        currentGoal.set(null);
        currentPath.set(null);
        robot.stop();
    }

    @Scheduled(fixedRate = 100)
    public void controlLoop() {
        RobotPosition goal = currentGoal.get();
        if (goal == null) return;

        // Phase 21: If robot supports high-level navigation, delegate and stop loop.
        if (robot.getCapabilities().supportedCommandTypes().contains("NAV2")) {
            log.info("Delegating navigation to Robot high-level Nav2 stack");
            robot.navigateTo(goal);
            currentGoal.set(null);
            return;
        }

        // Phase 17: Update SLAM before control
        robot.updateSlam();

        RobotState state = robot.getState();
        if (state.status() == RobotStatus.ERROR || state.status() == RobotStatus.PAUSED) return;

        // Use estimated pose for navigation decisions (Phase 17)
        RobotPosition currentPos = robot.getEstimatedPosition();
        Orientation currentOrientation = robot.getEstimatedOrientation();
        
        // Construct a virtual state for the controller using estimated data
        RobotState estimatedState = new RobotState(
                state.robotId(), state.houseId(), currentPos, currentOrientation, 
                state.velocity(), state.battery(), state.status(), state.timestamp()
        );

        // 1. Path Planning (or replanning if path is invalid/blocked)
        List<RobotPosition> path = currentPath.get();
        if (path == null || path.isEmpty()) {
            replanningRequired();
            path = currentPath.get();
            if (path == null || path.isEmpty()) return;
        }

        // 2. Obstacle Detection (Phase 14)
        if (robot.getMotionState().isColliding()) {
            log.warn("Obstacle detected! Stopping and replanning.");
            robot.stop();
            replanningRequired();
            return;
        }

        // 3. Motion Control
        MotionCommand cmd = motionController.nextCommand(estimatedState, new Path(path));
        if (cmd.duration().isZero()) {
            // Waypoint reached
            path.remove(0);
            if (path.isEmpty()) {
                log.info("Waypoint reached!");
                advanceGoal();
            }
        } else {
            robot.move(cmd);
        }
    }

    private void advanceGoal() {
        RobotPosition next = goalQueue.poll();
        currentGoal.set(next);
        if (next == null) {
            log.info("All goals reached!");
            currentPath.set(null);
            robot.stop();
        } else {
            replanningRequired();
        }
    }

    private void replanningRequired() {
        RobotPosition goal = currentGoal.get();
        if (goal == null) return;

        RobotState state = robot.getState();
        if (state.houseId() == null) {
            log.warn("Cannot plan path: Robot has no associated houseId");
            return;
        }

        try {
            // Phase 17: Use the map built by SLAM if available, otherwise fallback to server map
            OccupancyGrid grid = robot.getSlamMap();
            if (grid == null) {
                var mapDto = mapService.getLatest(state.houseId());
                grid = new OccupancyGrid(
                        mapDto.gridWidth(), mapDto.gridHeight(), mapDto.cellSize(), mapDto.mapData());
            }
            
            Path path = pathPlanner.findPath(robot.getEstimatedPosition(), goal, grid);
            if (path.isEmpty()) {
                log.error("No path found to goal!");
                advanceGoal();
            } else {
                currentPath.set(new ArrayList<>(path.points()));
            }
        } catch (Exception e) {
            log.error("Replanning failed: {}", e.getMessage());
        }
    }
}
