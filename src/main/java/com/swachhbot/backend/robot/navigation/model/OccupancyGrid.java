package com.swachhbot.backend.robot.navigation.model;

import lombok.Getter;

/**
 * In-memory representation of the occupancy grid for navigation.
 */
public class OccupancyGrid {
    @Getter private final int width;
    @Getter private final int height;
    @Getter private final double cellSize;
    private final char[] data;

    public enum CellType {
        UNKNOWN('U'),
        FREE('F'),
        OBSTACLE('O'),
        CLEANED('C');

        private final char code;
        CellType(char code) { this.code = code; }
        public static CellType fromCode(char code) {
            for (CellType t : values()) if (t.code == code) return t;
            return UNKNOWN;
        }
    }

    public OccupancyGrid(int width, int height, double cellSize, String mapData) {
        this.width = width;
        this.height = height;
        this.cellSize = cellSize;
        this.data = mapData.toCharArray();
    }

    public CellType getCell(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return CellType.OBSTACLE;
        return CellType.fromCode(data[y * width + x]);
    }

    public void setCell(int x, int y, CellType type) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            data[y * width + x] = type.code;
        }
    }

    public String getSerializedData() {
        return new String(data);
    }

    public boolean isTraversable(int x, int y) {
        CellType type = getCell(x, y);
        return type == CellType.FREE || type == CellType.CLEANED;
    }

    public int worldToGridX(double x) {
        return (int) (x / cellSize);
    }

    public int worldToGridY(double y) {
        return (int) (y / cellSize);
    }

    public double gridToWorldX(int x) {
        return (x + 0.5) * cellSize;
    }

    public double gridToWorldY(int y) {
        return (y + 0.5) * cellSize;
    }
}
