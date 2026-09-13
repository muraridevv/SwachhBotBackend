package com.swachhbot.backend.robot.hardware;

/**
 * Robot-side abstraction for the motor driver.
 */
public interface MotorController {
    /** 
     * Sets target velocities.
     * @param linear  mm/s
     * @param angular deg/s
     */
    void setVelocity(double linear, double angular);

    /** Immediate stop. */
    void stop();
}
