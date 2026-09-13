package com.swachhbot.backend.robot.sensors;

import com.swachhbot.backend.robot.model.SensorReading;
import java.util.Map;

/**
 * Hardware-independent distance sensor (e.g. Ultrasonic, LiDAR).
 */
public interface DistanceSensor {
    /** 
     * Returns distances in mm, keyed by direction or angle.
     * e.g. {"front": 120.5, "left": 400.0} 
     */
    SensorReading<Map<String, Double>> read();
}
