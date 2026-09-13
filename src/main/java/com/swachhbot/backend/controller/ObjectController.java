package com.swachhbot.backend.controller;

import com.swachhbot.backend.dto.ObjectDtos.ObjectDto;
import com.swachhbot.backend.dto.ObjectDtos.ObjectUpsertRequest;
import com.swachhbot.backend.service.ObjectService;
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

    @GetMapping
    public List<ObjectDto> list(@PathVariable UUID houseId) {
        return objectService.findByHouse(houseId);
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
