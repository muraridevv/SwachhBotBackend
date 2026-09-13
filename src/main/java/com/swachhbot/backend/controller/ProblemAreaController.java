package com.swachhbot.backend.controller;

import com.swachhbot.backend.dto.SessionDtos.ProblemAreaDto;
import com.swachhbot.backend.dto.SessionDtos.ProblemAreaRequest;
import com.swachhbot.backend.service.ProblemAreaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/houses/{houseId}/problems")
@RequiredArgsConstructor
public class ProblemAreaController {

    private final ProblemAreaService problemAreaService;

    @GetMapping
    public List<ProblemAreaDto> list(@PathVariable UUID houseId) {
        return problemAreaService.findByHouse(houseId);
    }

    @PostMapping
    public ResponseEntity<ProblemAreaDto> report(@PathVariable UUID houseId,
                                                 @Valid @RequestBody ProblemAreaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(problemAreaService.report(houseId, request));
    }
}
