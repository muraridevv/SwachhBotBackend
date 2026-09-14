package com.swachhbot.backend.service;

import com.swachhbot.backend.domain.House;
import com.swachhbot.backend.domain.RobotObject;
import com.swachhbot.backend.domain.vision.EnvironmentChange;
import com.swachhbot.backend.domain.enums.ObjectStatus;
import com.swachhbot.backend.dto.ObjectDtos.ObjectDto;
import com.swachhbot.backend.dto.ObjectDtos.EnvironmentChangeDto;
import com.swachhbot.backend.dto.ObjectDtos.ObjectUpsertRequest;
import com.swachhbot.backend.repository.EnvironmentChangeRepository;
import com.swachhbot.backend.repository.HouseRepository;
import com.swachhbot.backend.repository.RobotObjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ObjectService {

    private final RobotObjectRepository objectRepository;
    private final HouseRepository houseRepository;
    private final EnvironmentChangeRepository changeRepository;

    private static final double MOVE_THRESHOLD_MM = 100.0; // 10cm
    private static final double CONFIDENCE_THRESHOLD = 0.6;

    @Transactional(readOnly = true)
    public List<ObjectDto> findByHouse(UUID houseId) {
        return objectRepository.findByHouseId(houseId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<EnvironmentChangeDto> findChangesByHouse(UUID houseId) {
        return changeRepository.findByHouseIdOrderByDetectedAtDesc(houseId).stream()
                .map(change -> new EnvironmentChangeDto(
                        change.getId(), change.getHouse().getId(), change.getObject().getId(),
                        change.getChangeType(), change.getDescription(), change.getConfidence(), change.getDetectedAt()))
                .toList();
    }

    /** Idempotent upsert with change detection (Phase 18). */
    public ObjectDto upsert(UUID houseId, ObjectUpsertRequest request) {
        if (request.confidence() < CONFIDENCE_THRESHOLD) {
            log.debug("Ignoring low-confidence detection: {} ({})", request.type(), request.confidence());
            // A REST controller must never serialize a null body for a valid
            // detection request. Return a faithful, non-persisted echo instead.
            return new ObjectDto(request.id(), houseId, request.type(), request.category(), request.status(),
                    request.roomName(), request.x(), request.y(), request.confidence(),
                    request.firstDetected(), request.lastDetected(), request.detectionCount());
        }

        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new ResourceNotFoundException("House not found: " + houseId));

        RobotObject entity = objectRepository.findById(request.id())
                .orElseGet(() -> {
                    log.info("New object discovered: {} in house {}", request.type(), houseId);
                    RobotObject newObj = RobotObject.builder()
                            .id(request.id())
                            .house(house)
                            .type(request.type())
                            .category(request.category())
                            .status(request.status())
                            .firstDetected(Instant.now())
                            .lastDetected(Instant.now())
                            .detectionCount(0) // Will be incremented later if needed, or set by request
                            .build();
                    return objectRepository.save(newObj);
                });

        detectAndRecordChanges(entity, request, house);

        entity.setType(request.type());
        entity.setCategory(request.category());
        entity.setStatus(request.status());
        entity.setRoomName(request.roomName());
        
        // Track position history
        if (entity.getX() != 0 || entity.getY() != 0) {
            entity.setPreviousX(entity.getX());
            entity.setPreviousY(entity.getY());
        }
        
        entity.setX(request.x());
        entity.setY(request.y());
        entity.setConfidence(request.confidence());
        entity.setFirstDetected(request.firstDetected() != null ? request.firstDetected() : entity.getFirstDetected());
        entity.setLastDetected(request.lastDetected());
        entity.setDetectionCount(request.detectionCount());

        if (entity.getFirstDetected() == null) {
            entity.setFirstDetected(Instant.now());
        }

        return toDto(objectRepository.save(entity));
    }

    private void detectAndRecordChanges(RobotObject existing, ObjectUpsertRequest update, House house) {
        if (existing.getX() == 0 && existing.getY() == 0) {
            recordChange(house, existing, "ADDED", "New " + update.type() + " detected at " + update.roomName(), update.confidence());
            return;
        }

        double distance = Math.sqrt(Math.pow(existing.getX() - update.x(), 2) + Math.pow(existing.getY() - update.y(), 2));
        if (distance > MOVE_THRESHOLD_MM) {
            String desc = "Object '%s' moved %.1f mm (from %s to %s)".formatted(
                    existing.getType(), distance, existing.getRoomName(), update.roomName());
            recordChange(house, existing, "MOVED", desc, update.confidence());
            log.info("Environment change detected: {}", desc);
        }
    }

    private void recordChange(House house, RobotObject obj, String type, String desc, double confidence) {
        changeRepository.save(EnvironmentChange.builder()
                .house(house)
                .object(obj)
                .changeType(type)
                .description(desc)
                .confidence(confidence)
                .detectedAt(Instant.now())
                .build());
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
                o.getRoomName(),
                o.getX(),
                o.getY(),
                o.getConfidence(),
                o.getFirstDetected(),
                o.getLastDetected(),
                o.getDetectionCount()
        );
    }
}
