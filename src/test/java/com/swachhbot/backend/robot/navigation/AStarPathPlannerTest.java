package com.swachhbot.backend.robot.navigation;

import com.swachhbot.backend.robot.model.RobotPosition;
import com.swachhbot.backend.robot.navigation.model.OccupancyGrid;
import com.swachhbot.backend.robot.navigation.model.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AStarPathPlannerTest {

    @Test
    void shouldFindShortestPathInEmptyGrid() {
        AStarPathPlanner planner = new AStarPathPlanner();
        OccupancyGrid grid = new OccupancyGrid(10, 10, 100.0, "FFFFFFFFFF".repeat(10));
        
        RobotPosition start = new RobotPosition(50.0, 50.0);   // (0,0) grid
        RobotPosition goal = new RobotPosition(250.0, 250.0); // (2,2) grid
        
        Path path = planner.findPath(start, goal, grid);
        
        assertThat(path.points()).isNotEmpty();
        assertThat(path.points().get(0)).isEqualTo(start);
        assertThat(path.points().get(path.points().size() - 1)).isEqualTo(goal);
    }

    @Test
    void shouldAvoidObstacles() {
        AStarPathPlanner planner = new AStarPathPlanner();
        // Obstacle at (1,0), (1,1), (1,2) blocking direct X path
        String mapData = "FOFFFFFFFF" +
                         "FOFFFFFFFF" +
                         "FOFFFFFFFF" +
                         "FFFFFFFFFF".repeat(7);
        OccupancyGrid grid = new OccupancyGrid(10, 10, 100.0, mapData);

        RobotPosition start = new RobotPosition(50.0, 50.0);   // (0,0)
        RobotPosition goal = new RobotPosition(250.0, 50.0);  // (2,0)

        Path path = planner.findPath(start, goal, grid);

        assertThat(path.points()).isNotEmpty();
        // Should have more than 2 points because it must go around
        assertThat(path.points().size()).isGreaterThan(2);
        for (RobotPosition p : path.points()) {
            assertThat(grid.getCell(grid.worldToGridX(p.x()), grid.worldToGridY(p.y())))
                    .isNotEqualTo(OccupancyGrid.CellType.OBSTACLE);
        }
    }

    @Test
    void shouldReturnEmptyPathIfGoalIsUnreachable() {
        AStarPathPlanner planner = new AStarPathPlanner();
        // Fully blocked
        String mapData = "FOFFFFFFFF" +
                         "FOFFFFFFFF" +
                         "FOFFFFFFFF" +
                         "FOFFFFFFFF" +
                         "FOFFFFFFFF" +
                         "FOFFFFFFFF" +
                         "FOFFFFFFFF" +
                         "FOFFFFFFFF" +
                         "FOFFFFFFFF" +
                         "FOFFFFFFFF";
        OccupancyGrid grid = new OccupancyGrid(10, 10, 100.0, mapData);

        RobotPosition start = new RobotPosition(50.0, 50.0);   // (0,0)
        RobotPosition goal = new RobotPosition(250.0, 50.0);  // (2,0)

        Path path = planner.findPath(start, goal, grid);

        assertThat(path.points()).isEmpty();
    }
}
