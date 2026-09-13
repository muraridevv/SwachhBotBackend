package com.swachhbot.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Raw WebSocket endpoint for real-time telemetry.
 *
 * <p>Clients connect to {@code /ws/telemetry} and receive JSON frames whenever
 * the robot state changes. The same broadcast channel is used by the Android app
 * and (later) the Raspberry Pi / ROS bridge.
 */
@Component
@Slf4j
public class TelemetryWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper;

    public TelemetryWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("Telemetry client connected: {} (total={})", session.getId(), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("Telemetry client disconnected: {} (total={})", session.getId(), sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Clients may send a "ping"; we simply echo a pong to keep the link warm.
        if ("ping".equalsIgnoreCase(message.getPayload())) {
            try {
                session.sendMessage(new TextMessage("pong"));
            } catch (Exception ignored) {
                // session will be cleaned up on close
            }
        }
    }

    /** Broadcast a JSON-serializable payload to every connected client. */
    public void broadcast(Object payload) {
        if (sessions.isEmpty()) {
            return;
        }
        final String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Failed to serialize telemetry payload", e);
            return;
        }
        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            } catch (Exception e) {
                log.warn("Failed to send telemetry to {}", session.getId(), e);
            }
        }
    }
}
