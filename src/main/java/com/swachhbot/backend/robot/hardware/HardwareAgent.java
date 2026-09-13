package com.swachhbot.backend.robot.hardware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swachhbot.backend.dto.RobotDtos.RobotCommandMessage;
import com.swachhbot.backend.dto.RobotDtos.RobotStateDto;
import com.swachhbot.backend.domain.enums.RobotStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lightweight agent intended to run on the Raspberry Pi.
 *
 * <p>Phase 20: Manages the command loop, motor control, and safety heartbeat.
 * In a real deployment, this would be a standalone process.
 */
@Slf4j
@RequiredArgsConstructor
public class HardwareAgent {

    private final String robotId;
    private final MotorController motorController;
    private final ObjectMapper objectMapper;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicLong lastCommandTime = new AtomicLong(0);
    private static final long COMMAND_TIMEOUT_MS = 500; // Safety timeout
    
    private RobotStatus status = RobotStatus.IDLE;

    public void start() {
        log.info("Starting SwachhBot Hardware Agent for {}", robotId);
        
        // Heartbeat / Safety Monitor
        scheduler.scheduleAtFixedRate(this::checkSafety, 100, 100, TimeUnit.MILLISECONDS);
        
        // State reporting would happen here (Phase 13 sensors)
    }

    public void stop() {
        log.info("Stopping Hardware Agent");
        motorController.stop();
        scheduler.shutdown();
    }

    /** 
     * Dispatches an incoming command from the backend. 
     * Called by the WebSocket client on the Pi.
     */
    public void handleIncomingMessage(String json) {
        try {
            RobotCommandMessage msg = objectMapper.readValue(json, RobotCommandMessage.class);
            if (!robotId.equals(msg.robotId())) return;

            lastCommandTime.set(System.currentTimeMillis());

            switch (msg.commandType()) {
                case "MOVE" -> handleMove(msg.payload());
                case "STOP" -> {
                    status = RobotStatus.IDLE;
                    motorController.stop();
                }
                case "PAUSE" -> {
                    status = RobotStatus.PAUSED;
                    motorController.stop();
                }
                default -> log.warn("Unknown command type: {}", msg.commandType());
            }
        } catch (Exception e) {
            log.error("Failed to parse command message", e);
        }
    }

    private void handleMove(String payload) {
        try {
            // Simplified JSON parsing for the MovePayload
            // In real app, use a dedicated record
            MovePayload p = objectMapper.readValue(payload, MovePayload.class);
            status = RobotStatus.CLEANING;
            motorController.setVelocity(p.linear(), p.angular());
        } catch (Exception e) {
            log.error("Failed to parse move payload", e);
        }
    }

    private void checkSafety() {
        if (status == RobotStatus.CLEANING && 
            (System.currentTimeMillis() - lastCommandTime.get() > COMMAND_TIMEOUT_MS)) {
            log.warn("Command timeout! Engaging safety stop.");
            motorController.stop();
            status = RobotStatus.IDLE;
        }
    }

    private record MovePayload(double linear, double angular, long duration) {}
}
