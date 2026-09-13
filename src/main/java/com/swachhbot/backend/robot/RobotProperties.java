package com.swachhbot.backend.robot;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "swachhbot.robot")
public class RobotProperties {
    /** simulation | physical */
    private String mode = "simulation";
    private String defaultRobotId = "swachhbot-01";
}
