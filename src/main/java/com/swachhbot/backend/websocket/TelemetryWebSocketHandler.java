package com.swachhbot.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swachhbot.backend.dto.RobotDtos.RobotCommandMessage;
import com.swachhbot.backend.dto.RobotDtos.RobotStateDto;
import com.swachhbot.backend.service.RobotStateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Raw WebSocket endpoint for real-time telemetry and commands.
 */
@Component
@Slf4j
public class TelemetryWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final Map<String, WebSocketSession> robotSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final RobotStateService stateService;

    public TelemetryWebSocketHandler(ObjectMapper objectMapper, @Lazy RobotStateService stateService) {
        this.objectMapper = objectMapper;
        this.stateService = stateService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("Telemetry client connected: {} (total={})", session.getId(), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        robotSessions.entrySet().removeIf(e -> e.getValue().equals(session));
        log.info("Telemetry client disconnected: {} (total={})", session.getId(), sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        if ("ping".equalsIgnoreCase(payload)) {
            try {
                session.sendMessage(new TextMessage("pong"));
            } catch (Exception ignored) {}
            return;
        }

        // Registration: "register:swachhbot-01"
        if (payload.startsWith("register:")) {
            String robotId = payload.substring(9);
            robotSessions.put(robotId, session);
            log.info("Robot {} registered on session {}", robotId, session.getId());
            return;
        }

        // Incoming telemetry from robot
        try {
            RobotStateDto state = objectMapper.readValue(payload, RobotStateDto.class);
            if (state.robotId() != null) {
                stateService.update(state);
            }
        } catch (Exception e) {
            // Not a telemetry message, or malformed
            log.trace("Received non-telemetry message: {}", payload);
        }
    }

    /** Broadcast to all (telemetry). */
    public void broadcast(Object payload) {
        if (sessions.isEmpty()) return;
        String json = toJson(payload);
        if (json == null) return;
        TextMessage msg = new TextMessage(json);
        sessions.forEach(s -> send(s, msg));
    }

    /** Direct command to a specific robot. */
    public void sendToRobot(String robotId, RobotCommandMessage command) {
        WebSocketSession session = robotSessions.get(robotId);
        if (session == null || !session.isOpen()) {
            log.warn("Cannot send command to robot {}: no active session", robotId);
            return;
        }
        String json = toJson(command);
        if (json != null) {
            send(session, new TextMessage(json));
        }
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Failed to serialize WebSocket payload", e);
            return null;
        }
    }

    private void send(WebSocketSession session, TextMessage msg) {
        try {
            if (session.isOpen()) {
                session.sendMessage(msg);
            }
        } catch (IOException e) {
            log.warn("Failed to send WebSocket message to {}", session.getId(), e);
        }
    }
}
