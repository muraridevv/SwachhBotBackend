package com.swachhbot.backend.robot.slam;

import com.swachhbot.backend.robot.model.SensorReading;
import com.swachhbot.backend.robot.navigation.model.OccupancyGrid;
import com.swachhbot.backend.robot.slam.model.Pose;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Simple ray-casting map builder for Phase 17.
 */
@Slf4j
public class SimpleMapBuilder implements MapBuilder {

    @Override
    public void updateMap(OccupancyGrid grid, Pose pose, SensorReading<Map<String, Double>> distanceData) {
        int originX = grid.worldToGridX(pose.position().x());
        int originY = grid.worldToGridY(pose.position().y());

        for (Map.Entry<String, Double> entry : distanceData.value().entrySet()) {
            double angle = parseAngle(entry.getKey(), pose.orientation().degrees());
            double distance = entry.getValue();

            // Cast ray from origin to distance
            castRay(grid, originX, originY, angle, distance);
        }
    }

    private void castRay(OccupancyGrid grid, int x0, int y0, double angle, double distance) {
        double rad = Math.toRadians(angle - 90);
        int targetX = grid.worldToGridX(grid.gridToWorldX(x0) + distance * Math.cos(rad));
        int targetY = grid.worldToGridY(grid.gridToWorldY(y0) + distance * Math.sin(rad));

        // Simplified Bresenham's or line stepping to mark FREE cells along the path
        // and OBSTACLE at the end if the distance is less than max sensor range.
        
        // For Phase 17, we just mark the target as OBSTACLE and the path as FREE.
        drawLine(grid, x0, y0, targetX, targetY);
    }

    private void drawLine(OccupancyGrid grid, int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int currX = x0;
        int currY = y0;

        while (true) {
            if (currX == x1 && currY == y1) {
                grid.setCell(currX, currY, OccupancyGrid.CellType.OBSTACLE);
                break;
            }
            
            // Don't overwrite obstacles with free space (simple persistence)
            if (grid.getCell(currX, currY) != OccupancyGrid.CellType.OBSTACLE) {
                grid.setCell(currX, currY, OccupancyGrid.CellType.FREE);
            }

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                currX += sx;
            }
            if (e2 < dx) {
                err += dx;
                currY += sy;
            }
        }
    }

    private double parseAngle(String direction, double robotHeading) {
        return switch (direction.toLowerCase()) {
            case "front" -> robotHeading;
            case "rear" -> (robotHeading + 180) % 360;
            case "left" -> (robotHeading + 270) % 360;
            case "right" -> (robotHeading + 90) % 360;
            default -> {
                try {
                    yield (Double.parseDouble(direction) + robotHeading) % 360;
                } catch (NumberFormatException e) {
                    yield robotHeading;
                }
            }
        };
    }
}
