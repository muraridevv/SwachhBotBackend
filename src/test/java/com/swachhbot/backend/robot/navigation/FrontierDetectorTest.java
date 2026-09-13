package com.swachhbot.backend.robot.navigation;

import com.swachhbot.backend.robot.model.RobotPosition;
import com.swachhbot.backend.robot.navigation.model.OccupancyGrid;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FrontierDetectorTest {

    @Test
    void shouldDetectFrontiers() {
        FrontierDetector detector = new FrontierDetector();
        
        // 5x5 grid with some known FREE and some UNKNOWN
        // FFFFF
        // FFFFF
        // FFFFF
        // UUUUU
        // UUUUU
        String mapData = "FFFFF" + "FFFFF" + "FFFFF" + "UUUUU" + "UUUUU";
        OccupancyGrid grid = new OccupancyGrid(5, 5, 100.0, mapData);

        List<RobotPosition> frontiers = detector.detectFrontiers(grid);

        assertThat(frontiers).isNotEmpty();
        // Frontiers should be on the boundary between row 2 and 3
        for (RobotPosition f : frontiers) {
            assertThat(f.y()).isGreaterThanOrEqualTo(200.0);
            assertThat(f.y()).isLessThanOrEqualTo(300.0);
        }
    }

    @Test
    void shouldClusterFrontiers() {
        FrontierDetector detector = new FrontierDetector();
        
        // Two separate islands of unknown space, separated by enough FREE rows
        // to ensure frontiers don't touch even with 8-connectivity.
        
        String freeRow = "F".repeat(20);
        String unkRow  = "FFFFF" + "UUUUU" + "FFFFFFFFFF";
        
        StringBuilder sb = new StringBuilder();
        sb.append(freeRow);
        sb.append(unkRow);
        sb.append(freeRow);
        sb.append(freeRow); // Gap
        sb.append(freeRow); // Gap
        sb.append(unkRow);
        sb.append(freeRow);
        
        OccupancyGrid grid = new OccupancyGrid(20, 7, 100.0, sb.toString());

        List<RobotPosition> frontiers = detector.detectFrontiers(grid);

        // Should have exactly 2 distinct frontier centroids
        assertThat(frontiers.size()).isEqualTo(2);
    }
}
