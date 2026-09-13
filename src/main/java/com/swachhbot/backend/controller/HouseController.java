package com.swachhbot.backend.controller;

import com.swachhbot.backend.dto.HouseDtos.*;
import com.swachhbot.backend.service.HouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/houses")
@RequiredArgsConstructor
public class HouseController {

    private final HouseService houseService;

    @GetMapping
    public List<HouseDto> list() {
        return houseService.findAll();
    }

    @GetMapping("/{id}")
    public HouseDto get(@PathVariable UUID id) {
        return houseService.findById(id);
    }

    @PostMapping
    public ResponseEntity<HouseDto> create(@Valid @RequestBody HouseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(houseService.create(request));
    }

    @PutMapping("/{id}")
    public HouseDto update(@PathVariable UUID id, @Valid @RequestBody HouseRequest request) {
        return houseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        houseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ----- Nested rooms -----

    @PostMapping("/{houseId}/rooms")
    public ResponseEntity<RoomDto> addRoom(@PathVariable UUID houseId,
                                           @Valid @RequestBody RoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(houseService.addRoom(houseId, request));
    }
}
