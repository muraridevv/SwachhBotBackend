package com.swachhbot.backend.robot;

import com.swachhbot.backend.robot.slam.SlamEngineFactory;
import com.swachhbot.backend.service.CommandService;
import com.swachhbot.backend.service.RobotStateService;
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
    public Robot physicalRobot(RobotProperties properties, SlamEngineFactory slamFactory) {
        log.info("Configuring SwachhBot in PHYSICAL mode");
        return new PhysicalRobot(properties.getDefaultRobotId(), slamFactory);
    }
}
