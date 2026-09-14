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
    private static final double REACHED_THRESHOLD = 15.0; // Increased for robustness
    private static final double ROTATION_THRESHOLD = 2.0; // Tighter tolerance
    private static final double LINEAR_SPEED = 250.0;     // mm/s
    private static final double ANGULAR_SPEED = 120.0;    // deg/s

    public MotionCommand nextCommand(RobotState currentState, Path path) {
        if (path.isEmpty()) return new MotionCommand(0, 0, Duration.ZERO);

        RobotPosition target = path.points().get(0);
        double dx = target.x() - currentState.position().x();
        double dy = target.y() - currentState.position().y();
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < REACHED_THRESHOLD) {
            return new MotionCommand(0, 0, Duration.ZERO);
        }

        double targetAngle = Math.toDegrees(Math.atan2(dy, dx)) + 90;
        if (targetAngle < 0) targetAngle += 360;
        targetAngle %= 360;

        double currentAngle = currentState.orientation().degrees();
        double angleDiff = targetAngle - currentAngle;
        while (angleDiff > 180) angleDiff -= 360;
        while (angleDiff < -180) angleDiff += 360;

        if (Math.abs(angleDiff) > 30.0) {
            // Large turn: stop and rotate in place
            double direction = Math.signum(angleDiff);
            return new MotionCommand(0, direction * ANGULAR_SPEED, Duration.ofMillis(100));
        } else if (Math.abs(angleDiff) > ROTATION_THRESHOLD) {
            // Medium turn: slow move while rotating
            double direction = Math.signum(angleDiff);
            return new MotionCommand(LINEAR_SPEED * 0.4, direction * ANGULAR_SPEED, Duration.ofMillis(100));
        } else {
            // Small correction: full speed
            return new MotionCommand(LINEAR_SPEED, angleDiff * 3.0, Duration.ofMillis(100));
        }
    }
}
