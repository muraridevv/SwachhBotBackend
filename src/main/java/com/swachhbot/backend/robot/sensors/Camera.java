package com.swachhbot.backend.robot.sensors;

import com.swachhbot.backend.robot.model.SensorReading;

/**
 * Camera abstraction for image processing and object detection.
 */
public interface Camera {
    /** Returns the latest frame as a byte array (e.g. JPEG) or a resource URI. */
    SensorReading<byte[]> captureFrame();

    /** Returns simulated or real object detection results. */
    SensorReading<String> getDetections();
}
