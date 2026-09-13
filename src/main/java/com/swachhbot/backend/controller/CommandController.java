package com.swachhbot.backend.controller;

import com.swachhbot.backend.dto.RobotDtos.CommandAckRequest;
import com.swachhbot.backend.dto.RobotDtos.CommandDto;
import com.swachhbot.backend.dto.RobotDtos.CommandRequest;
import com.swachhbot.backend.service.CommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/commands")
@RequiredArgsConstructor
public class CommandController {

    private final CommandService commandService;

    @GetMapping
    public List<CommandDto> findByRobot(@RequestParam String robotId) {
        return commandService.findByRobot(robotId);
    }

    @PostMapping
    public ResponseEntity<CommandDto> issue(@Valid @RequestBody CommandRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commandService.issue(request));
    }

    /** Robot acknowledges a command it received over the WebSocket channel. */
    @PostMapping("/{id}/ack")
    public CommandDto ack(@PathVariable UUID id, @Valid @RequestBody CommandAckRequest request) {
        return commandService.acknowledge(id, request);
    }
}
