package com.swachhbot.backend.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunable safety + behaviour limits for the AI planning layer.
 * All caps are enforced in {@code PlanValidator}, not by the model.
 */
@ConfigurationProperties(prefix = "swachhbot.ai")
public class AiProperties {

    /** When false, the deterministic rule-based planner is used instead of the LLM. */
    private boolean enabled = true;

    /** Number of knowledge documents retrieved for retrieval-augmented generation. */
    private int topK = 6;

    /** Hard cap on the number of cleaning passes the AI may request. */
    private int maxPasses = 3;

    /** Hard cap on how many rooms a single plan may contain. */
    private int maxPlannedRooms = 12;

    /** Hard cap on the estimated duration (seconds) of a single plan. */
    private long maxDurationSeconds = 7200;

    /** Robot that receives plans when the caller does not specify one. */
    private String defaultRobotId = "swachhbot-01";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public int getMaxPasses() {
        return maxPasses;
    }

    public void setMaxPasses(int maxPasses) {
        this.maxPasses = maxPasses;
    }

    public int getMaxPlannedRooms() {
        return maxPlannedRooms;
    }

    public void setMaxPlannedRooms(int maxPlannedRooms) {
        this.maxPlannedRooms = maxPlannedRooms;
    }

    public long getMaxDurationSeconds() {
        return maxDurationSeconds;
    }

    public void setMaxDurationSeconds(long maxDurationSeconds) {
        this.maxDurationSeconds = maxDurationSeconds;
    }

    public String getDefaultRobotId() {
        return defaultRobotId;
    }

    public void setDefaultRobotId(String defaultRobotId) {
        this.defaultRobotId = defaultRobotId;
    }
}
