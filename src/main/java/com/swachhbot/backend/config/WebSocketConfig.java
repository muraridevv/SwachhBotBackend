package com.swachhbot.backend.config;

import com.swachhbot.backend.websocket.TelemetryWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * Registers two transports:
 * <ul>
 *   <li><b>Raw WebSocket</b> ({@code /ws/telemetry}) — lightweight, used by the
 *       Android client and the future Raspberry Pi / ROS bridge.</li>
 *   <li><b>STOMP</b> ({@code /ws/stomp}) — for browser dashboards that subscribe
 *       to {@code /topic/...} destinations.</li>
 * </ul>
 */
@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer, WebSocketMessageBrokerConfigurer {

    private final TelemetryWebSocketHandler telemetryHandler;

    @Value("${swachhbot.websocket.telemetry-endpoint:/ws/telemetry}")
    private String telemetryEndpoint;

    @Value("${swachhbot.websocket.stomp-endpoint:/ws/stomp}")
    private String stompEndpoint;

    @Value("${swachhbot.websocket.allowed-origins:*}")
    private String allowedOrigins;

    // ----- Raw WebSocket -----

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(telemetryHandler, telemetryEndpoint)
                .setAllowedOrigins(allowedOrigins);
    }

    // ----- STOMP -----

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(stompEndpoint)
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();
    }
}
