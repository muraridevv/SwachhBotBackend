package com.swachhbot.backend.robot.model;

import java.time.Instant;

/**
 * Base record for any sensor reading.
 * @param <T> The type of the reading value.
 */
public record SensorReading<T>(
    Instant timestamp,
    String sensorId,
    T value,
    double confidence
) {
}
