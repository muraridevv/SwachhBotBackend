package com.swachhbot.backend.controller;

import com.swachhbot.backend.dto.RobotDtos.RobotStateDto;
import com.swachhbot.backend.service.RobotStateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/robots")
@RequiredArgsConstructor
public class RobotController {

    private final RobotStateService robotStateService;

    @GetMapping("/{robotId}/state")
    public RobotStateDto getState(@PathVariable String robotId) {
        return robotStateService.get(robotId);
    }

    /** Robots (or the Android simulator) push their live state here. */
    @PutMapping("/{robotId}/state")
    public RobotStateDto updateState(@PathVariable String robotId,
                                     @Valid @RequestBody RobotStateDto request) {
        // Ensure the path and body agree.
        RobotStateDto normalized = new RobotStateDto(
                robotId,
                request.houseId(),
                request.x(),
                request.y(),
                request.rotation(),
                request.velocity(),
                request.battery(),
                request.status(),
                request.isColliding(),
                request.updatedAt()
        );
        return robotStateService.update(normalized);
    }

    @PostMapping("/{robotId}/progress")
    public void publishProgress(@PathVariable String robotId,
                                @RequestParam double cleaningPercent) {
        robotStateService.publishProgress(robotId, cleaningPercent);
    }
}
