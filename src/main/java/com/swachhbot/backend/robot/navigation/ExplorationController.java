package com.swachhbot.backend.robot.navigation;

import com.swachhbot.backend.robot.Robot;
import com.swachhbot.backend.domain.enums.RobotStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exploration")
@RequiredArgsConstructor
public class ExplorationController {

    private final ExplorationService explorationService;
    private final Robot robot;

    @PostMapping("/start")
    public void start() {
        // We ensure the robot state is CLEANING so the NavigationEngine picks it up
        // (For physical/ROS bots, this acts as the high-level operational trigger)
        robot.executeCommand("START_CLEANING", "{\"source\": \"exploration\"}");
        explorationService.startExploration();
    }

    @PostMapping("/stop")
    public void stop() {
        explorationService.stopExploration();
        robot.stop();
    }

    @GetMapping("/status")
    public ExplorationService.ExplorationStatus getStatus() {
        return explorationService.getStatus();
    }
}
