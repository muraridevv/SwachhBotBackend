package com.swachhbot.backend.robot.slam;

import com.swachhbot.backend.robot.navigation.model.OccupancyGrid;
import org.springframework.stereotype.Component;

@Component
public class SlamEngineFactory {
    public SlamEngine create(int width, int height, double cellSize) {
        // Create an empty unknown grid
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < width * height; i++) sb.append('U');
        OccupancyGrid grid = new OccupancyGrid(width, height, cellSize, sb.toString());
        return new SimpleSlamEngine(grid);
    }
}
