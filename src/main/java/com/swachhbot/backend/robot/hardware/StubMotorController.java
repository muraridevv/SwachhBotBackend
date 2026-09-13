package com.swachhbot.backend.robot.hardware;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StubMotorController implements MotorController {
    @Override
    public void setVelocity(double linear, double angular) {
        log.info("[HARDWARE] Setting motors: linear={} mm/s, angular={} deg/s", linear, angular);
    }

    @Override
    public void stop() {
        log.info("[HARDWARE] Emergency STOP - Cutting motor power");
    }
}
