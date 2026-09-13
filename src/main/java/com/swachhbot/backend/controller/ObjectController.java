package com.swachhbot.backend.controller;

import com.swachhbot.backend.domain.vision.EnvironmentChange;
import com.swachhbot.backend.dto.ObjectDtos.ObjectDto;
import com.swachhbot.backend.dto.ObjectDtos.ObjectUpsertRequest;
import com.swachhbot.backend.repository.EnvironmentChangeRepository;
import com.swachhbot.backend.service.ObjectService;
import com.swachhbot.backend.service.vision.EnvironmentAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/houses/{houseId}/objects")
@RequiredArgsConstructor
public class ObjectController {

    private final ObjectService objectService;
    private final EnvironmentChangeRepository changeRepository;
    private final EnvironmentAnalysisService analysisService;

    @GetMapping
    public List<ObjectDto> list(@PathVariable UUID houseId) {
        return objectService.findByHouse(houseId);
    }

    @GetMapping("/changes")
    public List<EnvironmentChange> listChanges(@PathVariable UUID houseId) {
        return changeRepository.findByHouseIdOrderByDetectedAtDesc(houseId);
    }

    @GetMapping("/changes/explain")
    public String explainChanges(@PathVariable UUID houseId) {
        return analysisService.explainChanges(houseId);
    }

    @PutMapping
    public ObjectDto upsert(@PathVariable UUID houseId,
                            @Valid @RequestBody ObjectUpsertRequest request) {
        return objectService.upsert(houseId, request);
    }

    @DeleteMapping("/{objectId}")
    public ResponseEntity<Void> delete(@PathVariable UUID houseId,
                                       @PathVariable UUID objectId) {
        objectService.delete(objectId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
