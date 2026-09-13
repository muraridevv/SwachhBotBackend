package com.swachhbot.backend.websocket;

import com.swachhbot.backend.dto.RobotDtos.TelemetryMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Facade used by services to push telemetry without caring about transport.
 * Today it fans out over the raw WebSocket handler; a STOMP or MQTT sink
 * (e.g. for ROS) can be added later without touching the services.
 */
@Component
@RequiredArgsConstructor
public class TelemetryPublisher {

    private final TelemetryWebSocketHandler handler;

    public void publishTelemetry(TelemetryMessage message) {
        handler.broadcast(message);
    }
}
