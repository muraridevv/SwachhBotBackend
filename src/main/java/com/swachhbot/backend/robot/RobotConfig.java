package com.swachhbot.backend.robot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swachhbot.backend.robot.ros2.Ros2Robot;
import com.swachhbot.backend.robot.slam.SlamEngineFactory;
import com.swachhbot.backend.service.CommandService;
import com.swachhbot.backend.service.RobotStateService;
import com.swachhbot.backend.websocket.TelemetryWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RobotConfig {

    @Bean
    @ConditionalOnProperty(name = "swachhbot.robot.mode", havingValue = "simulation", matchIfMissing = true)
    public Robot simulationRobot(RobotProperties properties, RobotStateService stateService, CommandService commandService, SlamEngineFactory slamFactory) {
        log.info("Configuring SwachhBot in SIMULATION mode");
        return new SimulationRobot(properties.getDefaultRobotId(), stateService, commandService, slamFactory);
    }

    @Bean
    @ConditionalOnProperty(name = "swachhbot.robot.mode", havingValue = "physical")
    public Robot physicalRobot(RobotProperties properties, SlamEngineFactory slamFactory, TelemetryWebSocketHandler communicationHandler) {
        log.info("Configuring SwachhBot in PHYSICAL mode");
        return new PhysicalRobot(properties.getDefaultRobotId(), slamFactory, communicationHandler);
    }

    @Bean
    @ConditionalOnProperty(name = "swachhbot.robot.mode", havingValue = "ros2")
    public Robot ros2Robot(RobotProperties properties, SlamEngineFactory slamFactory, ObjectMapper objectMapper) {
        log.info("Configuring SwachhBot in ROS 2 mode (Bridge URL: {})", properties.getRosBridgeUrl());
        return new Ros2Robot(properties.getDefaultRobotId(), properties.getRosBridgeUrl(), objectMapper, slamFactory);
    }
}
