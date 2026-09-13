package com.swachhbot.backend.controller;

import com.swachhbot.backend.dto.MapDtos.MapDto;
import com.swachhbot.backend.dto.MapDtos.MapUpdateRequest;
import com.swachhbot.backend.service.MapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/houses/{houseId}/map")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

    @GetMapping
    public MapDto getLatest(@PathVariable UUID houseId) {
        return mapService.getLatest(houseId);
    }

    @PutMapping
    public MapDto save(@PathVariable UUID houseId,
                       @Valid @RequestBody MapUpdateRequest request) {
        return mapService.save(houseId, request);
    }
}
