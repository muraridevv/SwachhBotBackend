package com.swachhbot.backend.robot.navigation;

import com.swachhbot.backend.robot.model.MotionCommand;
import com.swachhbot.backend.robot.model.RobotPosition;
import com.swachhbot.backend.robot.model.RobotState;
import com.swachhbot.backend.robot.navigation.model.Path;

import java.time.Duration;

/**
 * Converts a path into a sequence of motion commands.
 */
public class MotionController {
    private static final double REACHED_THRESHOLD = 10.0; // mm
    private static final double ROTATION_THRESHOLD = 5.0; // degrees
    private static final double LINEAR_SPEED = 200.0;     // mm/s
    private static final double ANGULAR_SPEED = 45.0;    // deg/s

    public MotionCommand nextCommand(RobotState currentState, Path path) {
        if (path.isEmpty()) return new MotionCommand(0, 0, Duration.ZERO);

        RobotPosition target = path.points().get(0);
        double dx = target.x() - currentState.position().x();
        double dy = target.y() - currentState.position().y();
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < REACHED_THRESHOLD) {
            // Target reached, should move to next point in next call if path was updated
            return new MotionCommand(0, 0, Duration.ZERO);
        }

        // Calculate target heading
        // 0 degrees is UP (negative Y). Math.atan2(y, x) is 0 at RIGHT.
        // We need to map our coordinate system.
        // In the simulator: 0 deg is UP.
        double targetAngle = Math.toDegrees(Math.atan2(dy, dx)) + 90;
        if (targetAngle < 0) targetAngle += 360;
        targetAngle %= 360;

        double currentAngle = currentState.orientation().degrees();
        double angleDiff = targetAngle - currentAngle;
        while (angleDiff > 180) angleDiff -= 360;
        while (angleDiff < -180) angleDiff += 360;

        if (Math.abs(angleDiff) > ROTATION_THRESHOLD) {
            // Rotate first
            double direction = Math.signum(angleDiff);
            return new MotionCommand(0, direction * ANGULAR_SPEED, Duration.ofMillis(100));
        } else {
            // Move forward
            return new MotionCommand(LINEAR_SPEED, 0, Duration.ofMillis(100));
        }
    }
}
