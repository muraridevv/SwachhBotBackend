package com.swachhbot.backend.robot.navigation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exploration")
@RequiredArgsConstructor
public class ExplorationController {

    private final ExplorationService explorationService;

    @PostMapping("/start")
    public void start() {
        explorationService.startExploration();
    }

    @PostMapping("/stop")
    public void stop() {
        explorationService.stopExploration();
    }

    @GetMapping("/status")
    public ExplorationService.ExplorationStatus getStatus() {
        return explorationService.getStatus();
    }
}
