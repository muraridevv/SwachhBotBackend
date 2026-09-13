package com.swachhbot.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Central robot / AI server for SwachhBot.
 *
 * <p>Responsibilities:
 * <ul>
 *     <li>Persist house knowledge (houses, rooms, furniture, maps, objects).</li>
 *     <li>Store cleaning history and robot problems.</li>
 *     <li>Stream real-time telemetry (position, battery, status, progress).</li>
 *     <li>Relay cleaning commands to the robot (Android app today, Raspberry Pi/ROS later).</li>
 * </ul>
 */
@SpringBootApplication
@EnableScheduling
public class SwachhBotBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SwachhBotBackendApplication.class, args);
    }
}
