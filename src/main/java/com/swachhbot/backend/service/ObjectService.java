package com.swachhbot.backend.service;

import com.swachhbot.backend.domain.House;
import com.swachhbot.backend.domain.RobotObject;
import com.swachhbot.backend.dto.ObjectDtos.ObjectDto;
import com.swachhbot.backend.dto.ObjectDtos.ObjectUpsertRequest;
import com.swachhbot.backend.repository.HouseRepository;
import com.swachhbot.backend.repository.RobotObjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ObjectService {

    private final RobotObjectRepository objectRepository;
    private final HouseRepository houseRepository;

    @Transactional(readOnly = true)
    public List<ObjectDto> findByHouse(UUID houseId) {
        return objectRepository.findByHouseId(houseId).stream().map(this::toDto).toList();
    }

    /** Idempotent upsert keyed by the client-generated id. */
    public ObjectDto upsert(UUID houseId, ObjectUpsertRequest request) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new ResourceNotFoundException("House not found: " + houseId));

        RobotObject entity = objectRepository.findById(request.id())
                .orElseGet(() -> RobotObject.builder().id(request.id()).house(house).build());

        entity.setType(request.type());
        entity.setCategory(request.category());
        entity.setStatus(request.status());
        entity.setX(request.x());
        entity.setY(request.y());
        entity.setConfidence(request.confidence());
        entity.setFirstDetected(request.firstDetected());
        entity.setLastDetected(request.lastDetected());
        entity.setDetectionCount(request.detectionCount());

        return toDto(objectRepository.save(entity));
    }

    public void delete(UUID id) {
        objectRepository.deleteById(id);
    }

    private ObjectDto toDto(RobotObject o) {
        return new ObjectDto(
                o.getId(),
                o.getHouse().getId(),
                o.getType(),
                o.getCategory(),
                o.getStatus(),
                o.getX(),
                o.getY(),
                o.getConfidence(),
                o.getFirstDetected(),
                o.getLastDetected(),
                o.getDetectionCount()
        );
    }
}
