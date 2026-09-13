package com.swachhbot.backend.intelligence.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "swachhbot.intelligence")
public class CleaningIntelligenceProperties {

    /** Weight for dirty area score. */
    private double dirtWeight = 0.4;

    /** Weight for time elapsed since last cleaning. */
    private double recencyWeight = 0.3;

    /** Weight for user-defined priority. */
    private double userPriorityWeight = 0.2;

    /** Weight for obstacle frequency (negative impact on priority). */
    private double obstacleImpactWeight = 0.1;

    /** Base score for normalization. */
    private double baseScore = 10.0;
}
