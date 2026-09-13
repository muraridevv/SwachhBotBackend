package com.swachhbot.backend.robot.navigation;

import com.swachhbot.backend.robot.model.RobotPosition;
import com.swachhbot.backend.robot.navigation.model.OccupancyGrid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Detects frontiers (boundaries between known free space and unknown space).
 */
@Component
@Slf4j
public class FrontierDetector {

    public List<RobotPosition> detectFrontiers(OccupancyGrid grid) {
        List<GridPos> frontierCells = new ArrayList<>();
        int width = grid.getWidth();
        int height = grid.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (grid.getCell(x, y) == OccupancyGrid.CellType.FREE) {
                    if (hasUnknownNeighbor(x, y, grid)) {
                        frontierCells.add(new GridPos(x, y));
                    }
                }
            }
        }

        if (frontierCells.isEmpty()) {
            return Collections.emptyList();
        }

        // Group frontier cells into clusters (Frontier Regions)
        List<List<GridPos>> clusters = clusterFrontiers(frontierCells);
        
        // Convert cluster centroids to world coordinates
        List<RobotPosition> frontiers = new ArrayList<>();
        for (List<GridPos> cluster : clusters) {
            if (cluster.size() < 3) continue; // Filter out noise
            
            double sumX = 0;
            double sumY = 0;
            for (GridPos pos : cluster) {
                sumX += pos.x;
                sumY += pos.y;
            }
            int centerX = (int) (sumX / cluster.size());
            int centerY = (int) (sumY / cluster.size());
            
            frontiers.add(new RobotPosition(grid.gridToWorldX(centerX), grid.gridToWorldY(centerY)));
        }

        return frontiers;
    }

    private boolean hasUnknownNeighbor(int x, int y, OccupancyGrid grid) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                if (grid.getCell(x + dx, y + dy) == OccupancyGrid.CellType.UNKNOWN) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<List<GridPos>> clusterFrontiers(List<GridPos> cells) {
        List<List<GridPos>> clusters = new ArrayList<>();
        Set<GridPos> unvisited = new HashSet<>(cells);

        while (!unvisited.isEmpty()) {
            GridPos start = unvisited.iterator().next();
            List<GridPos> cluster = new ArrayList<>();
            Queue<GridPos> queue = new LinkedList<>();
            
            queue.add(start);
            unvisited.remove(start);

            while (!queue.isEmpty()) {
                GridPos current = queue.poll();
                cluster.add(current);

                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) continue;
                        GridPos neighbor = new GridPos(current.x + dx, current.y + dy);
                        if (unvisited.contains(neighbor)) {
                            unvisited.remove(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }
            clusters.add(cluster);
        }
        return clusters;
    }

    private record GridPos(int x, int y) {}
}
