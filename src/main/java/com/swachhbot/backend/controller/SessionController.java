package com.swachhbot.backend.controller;

import com.swachhbot.backend.dto.SessionDtos.SessionDto;
import com.swachhbot.backend.dto.SessionDtos.SessionRequest;
import com.swachhbot.backend.service.CleaningSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/houses/{houseId}/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final CleaningSessionService sessionService;

    @GetMapping
    public List<SessionDto> list(@PathVariable UUID houseId) {
        return sessionService.findByHouse(houseId);
    }

    @PostMapping
    public ResponseEntity<SessionDto> create(@PathVariable UUID houseId,
                                             @Valid @RequestBody SessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.create(houseId, request));
    }
}
