package com.swachhbot.backend.robot.navigation;

import com.swachhbot.backend.robot.model.RobotPosition;
import com.swachhbot.backend.robot.navigation.model.OccupancyGrid;
import com.swachhbot.backend.robot.navigation.model.Path;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.util.*;

public class AStarPathPlanner implements PathPlanner {

    @Override
    public Path findPath(RobotPosition start, RobotPosition goal, OccupancyGrid grid) {
        int startX = grid.worldToGridX(start.x());
        int startY = grid.worldToGridY(start.y());
        int goalX = grid.worldToGridX(goal.x());
        int goalY = grid.worldToGridY(goal.y());

        if (!grid.isTraversable(goalX, goalY)) {
            return new Path(Collections.emptyList());
        }

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<GridPos, Node> allNodes = new HashMap<>();

        Node startNode = new Node(new GridPos(startX, startY), 0, heuristic(startX, startY, goalX, goalY));
        openSet.add(startNode);
        allNodes.put(startNode.pos, startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.pos.x == goalX && current.pos.y == goalY) {
                return reconstructPath(current, grid);
            }

            for (GridPos neighborPos : getNeighbors(current.pos, grid)) {
                double tentativeGScore = current.gScore + distance(current.pos, neighborPos);
                Node neighbor = allNodes.get(neighborPos);

                if (neighbor == null || tentativeGScore < neighbor.gScore) {
                    if (neighbor == null) {
                        neighbor = new Node(neighborPos, tentativeGScore, heuristic(neighborPos.x, neighborPos.y, goalX, goalY));
                        allNodes.put(neighborPos, neighbor);
                    } else {
                        neighbor.gScore = tentativeGScore;
                        neighbor.fScore = neighbor.gScore + neighbor.hScore;
                    }
                    neighbor.parent = current;
                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }

        return new Path(Collections.emptyList());
    }

    private double heuristic(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    private double distance(GridPos a, GridPos b) {
        return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
    }

    private List<GridPos> getNeighbors(GridPos pos, OccupancyGrid grid) {
        List<GridPos> neighbors = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int nx = pos.x + dx;
                int ny = pos.y + dy;
                if (grid.isTraversable(nx, ny)) {
                    neighbors.add(new GridPos(nx, ny));
                }
            }
        }
        return neighbors;
    }

    private Path reconstructPath(Node goalNode, OccupancyGrid grid) {
        List<RobotPosition> points = new ArrayList<>();
        Node current = goalNode;
        while (current != null) {
            points.add(new RobotPosition(grid.gridToWorldX(current.pos.x), grid.gridToWorldY(current.pos.y)));
            current = current.parent;
        }
        Collections.reverse(points);
        return new Path(points);
    }

    @AllArgsConstructor
    @EqualsAndHashCode
    private static class GridPos {
        int x, y;
    }

    private static class Node {
        GridPos pos;
        double gScore;
        double hScore;
        double fScore;
        Node parent;

        Node(GridPos pos, double gScore, double hScore) {
            this.pos = pos;
            this.gScore = gScore;
            this.hScore = hScore;
            this.fScore = gScore + hScore;
        }
    }
}
