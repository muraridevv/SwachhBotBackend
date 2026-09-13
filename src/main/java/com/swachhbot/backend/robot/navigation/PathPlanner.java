package com.swachhbot.backend.robot.navigation;

import com.swachhbot.backend.robot.model.RobotPosition;
import com.swachhbot.backend.robot.navigation.model.OccupancyGrid;
import com.swachhbot.backend.robot.navigation.model.Path;

public interface PathPlanner {
    Path findPath(RobotPosition start, RobotPosition goal, OccupancyGrid grid);
}
