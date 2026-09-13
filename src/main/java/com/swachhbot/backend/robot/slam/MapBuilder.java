package com.swachhbot.backend.robot.slam;

import com.swachhbot.backend.robot.model.SensorReading;
import com.swachhbot.backend.robot.navigation.model.OccupancyGrid;
import com.swachhbot.backend.robot.slam.model.Pose;

import java.util.Map;

/**
 * Interface for building/updating an occupancy map from sensor observations.
 */
public interface MapBuilder {
    /** 
     * Updates the grid based on distance sensor observations from a specific pose.
     * @param grid The grid to update
     * @param pose The pose from which observations were made
     * @param distanceData Distance readings keyed by angle or direction
     */
    void updateMap(OccupancyGrid grid, Pose pose, SensorReading<Map<String, Double>> distanceData);
}
