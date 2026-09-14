package com.swachhbot.backend.service;

import com.swachhbot.backend.domain.House;
import com.swachhbot.backend.domain.RobotStateEntity;
import com.swachhbot.backend.dto.RobotDtos.RobotStateDto;
import com.swachhbot.backend.dto.RobotDtos.TelemetryMessage;
import com.swachhbot.backend.repository.HouseRepository;
import com.swachhbot.backend.repository.RobotStateRepository;
import com.swachhbot.backend.websocket.TelemetryPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class RobotStateService {

    private final RobotStateRepository robotStateRepository;
    private final HouseRepository houseRepository;
    private final TelemetryPublisher telemetryPublisher;

    @Transactional(readOnly = true)
    public RobotStateDto get(String robotId) {
        return robotStateRepository.findByRobotId(robotId)
                .map(this::toDto)
                .orElse(new RobotStateDto(robotId, null, 0, 0, 0, 0, 100, 
                        com.swachhbot.backend.domain.enums.RobotStatus.IDLE, false, Instant.now()));
    }

    /** Called by the robot (or by the Android simulator) to report its live state. */
    public RobotStateDto update(RobotStateDto incoming) {
        RobotStateEntity entity = robotStateRepository.findByRobotId(incoming.robotId())
                .orElseGet(() -> RobotStateEntity.builder().robotId(incoming.robotId()).build());

        if (incoming.houseId() != null) {
            House house = houseRepository.findById(incoming.houseId())
                    .orElseThrow(() -> new ResourceNotFoundException("House not found: " + incoming.houseId()));
            entity.setHouse(house);
        }

        entity.setX(incoming.x());
        entity.setY(incoming.y());
        entity.setRotation(incoming.rotation());
        entity.setVelocity(incoming.velocity());
        entity.setBattery(incoming.battery());
        entity.setStatus(incoming.status());

        RobotStateEntity saved = robotStateRepository.save(entity);
        RobotStateDto dto = toDto(saved);

        // Fan out to WebSocket clients in real time.
        telemetryPublisher.publishTelemetry(new TelemetryMessage(
                TelemetryMessage.TYPE,
                saved.getRobotId(),
                saved.getX(),
                saved.getY(),
                saved.getRotation(),
                saved.getVelocity(),
                saved.getBattery(),
                saved.getStatus(),
                incoming.isColliding(),
                null,
                Instant.now()
        ));

        return dto;
    }

    /** Used to attach cleaning progress to the telemetry stream. */
    public void publishProgress(String robotId, double cleaningPercent) {
        RobotStateEntity entity = findOrThrow(robotId);
        telemetryPublisher.publishTelemetry(new TelemetryMessage(
                TelemetryMessage.TYPE,
                entity.getRobotId(),
                entity.getX(),
                entity.getY(),
                entity.getRotation(),
                entity.getVelocity(),
                entity.getBattery(),
                entity.getStatus(),
                false, // isColliding (not tracked in persistent state)
                cleaningPercent,
                Instant.now()
        ));
    }

    private RobotStateEntity findOrThrow(String robotId) {
        return robotStateRepository.findByRobotId(robotId)
                .orElseThrow(() -> new ResourceNotFoundException("Robot not found: " + robotId));
    }

    private RobotStateDto toDto(RobotStateEntity e) {
        return new RobotStateDto(
                e.getRobotId(),
                e.getHouse() != null ? e.getHouse().getId() : null,
                e.getX(),
                e.getY(),
                e.getRotation(),
                e.getVelocity(),
                e.getBattery(),
                e.getStatus(),
                false, // Persistent state doesn't track real-time collision
                e.getUpdatedAt()
        );
    }
}
