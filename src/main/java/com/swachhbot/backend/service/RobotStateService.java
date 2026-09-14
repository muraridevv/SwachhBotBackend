package com.swachhbot.backend.service;

import com.swachhbot.backend.domain.House;
import com.swachhbot.backend.domain.RobotStateEntity;
import com.swachhbot.backend.dto.RobotDtos.RobotStateDto;
import com.swachhbot.backend.dto.RobotDtos.TelemetryMessage;
import com.swachhbot.backend.repository.HouseRepository;
import com.swachhbot.backend.repository.RobotStateRepository;
import com.swachhbot.backend.robot.RobotProperties;
import com.swachhbot.backend.websocket.TelemetryPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class RobotStateService {

    private final RobotStateRepository robotStateRepository;
    private final HouseRepository houseRepository;
    private final TelemetryPublisher telemetryPublisher;
    private final RobotProperties robotProperties;

    /** Advances in-process cleaning runs so simulation mode has visible telemetry. */
    @Scheduled(fixedRate = 250)
    public void advanceSimulation() {
        if (!"simulation".equalsIgnoreCase(robotProperties.getMode())) return;
        robotStateRepository.findByStatus(com.swachhbot.backend.domain.enums.RobotStatus.CLEANING).forEach(this::advanceCleaningRobot);
        robotStateRepository.findByStatus(com.swachhbot.backend.domain.enums.RobotStatus.RETURNING).forEach(this::returnToDock);
    }

    private void advanceCleaningRobot(RobotStateEntity state) {
            double heading = Math.toRadians(state.getRotation());
            double maxX = state.getHouse() == null ? Double.MAX_VALUE : Math.max(125, state.getHouse().getWidth() - 125);
            double maxY = state.getHouse() == null ? Double.MAX_VALUE : Math.max(125, state.getHouse().getHeight() - 125);
            state.setX(Math.max(125, Math.min(maxX, state.getX() + 25 * Math.cos(heading))));
            state.setY(Math.max(125, Math.min(maxY, state.getY() + 25 * Math.sin(heading))));
            state.setVelocity(100);
            state.setBattery(Math.max(0, state.getBattery() - 0.01));
            RobotStateEntity saved = robotStateRepository.save(state);
            telemetryPublisher.publishTelemetry(new TelemetryMessage(TelemetryMessage.TYPE, saved.getRobotId(),
                    saved.getX(), saved.getY(), saved.getRotation(), saved.getVelocity(), saved.getBattery(),
                    saved.getStatus(), false, null, Instant.now()));
    }

    private void returnToDock(RobotStateEntity state) {
        double dx = 125 - state.getX(), dy = 125 - state.getY(), distance = Math.hypot(dx, dy);
        if (distance <= 25) { state.setX(125); state.setY(125); state.setVelocity(0); state.setStatus(com.swachhbot.backend.domain.enums.RobotStatus.IDLE); }
        else { state.setX(state.getX() + 25 * dx / distance); state.setY(state.getY() + 25 * dy / distance); state.setVelocity(100); }
        RobotStateEntity saved = robotStateRepository.save(state);
        telemetryPublisher.publishTelemetry(new TelemetryMessage(TelemetryMessage.TYPE, saved.getRobotId(), saved.getX(), saved.getY(), saved.getRotation(), saved.getVelocity(), saved.getBattery(), saved.getStatus(), false, null, Instant.now()));
    }

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
