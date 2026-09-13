package com.swachhbot.backend.robot;

import com.swachhbot.backend.domain.enums.RobotStatus;
import com.swachhbot.backend.robot.model.*;
import com.swachhbot.backend.dto.RobotDtos.RobotCommandMessage;
import com.swachhbot.backend.robot.sensors.*;
import com.swachhbot.backend.robot.sensors.physical.*;
import com.swachhbot.backend.robot.slam.SlamEngine;
import com.swachhbot.backend.robot.slam.SlamEngineFactory;
import com.swachhbot.backend.robot.navigation.model.OccupancyGrid;
import com.swachhbot.backend.websocket.TelemetryWebSocketHandler;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Digital Twin for the actual physical hardware (Raspberry Pi).
 */
@Slf4j
public class PhysicalRobot implements Robot {

    private final String robotId;
    private final SlamEngineFactory slamFactory;
    private final TelemetryWebSocketHandler communicationHandler;

    // Sensors
    private final DistanceSensor distanceSensor;
    private final Camera camera;
    private final IMU imu;
    private final WheelEncoder wheelEncoder;
    private final BatterySensor batterySensor;
    private final CliffSensor cliffSensor;

    // SLAM
    private final AtomicReference<SlamEngine> slamEngine = new AtomicReference<>();

    public PhysicalRobot(String robotId, SlamEngineFactory slamFactory, TelemetryWebSocketHandler communicationHandler) {
        this.robotId = robotId;
        this.slamFactory = slamFactory;
        this.communicationHandler = communicationHandler;
        
        // Initialize physical sensor stubs
        this.distanceSensor = new PhysicalDistanceSensor(robotId + "-distance");
        this.camera = new PhysicalCamera(robotId + "-camera");
        this.imu = new PhysicalIMU(robotId + "-imu");
        this.wheelEncoder = new PhysicalWheelEncoder(robotId + "-encoder");
        this.batterySensor = new PhysicalBatterySensor(robotId + "-battery");
        this.cliffSensor = new PhysicalCliffSensor(robotId + "-cliff");
    }

    @Override
    public String getRobotId() {
        return robotId;
    }

    @Override
    public RobotState getState() {
        log.warn("PhysicalRobot.getState() called - reporting stub state");
        return new RobotState(
                robotId,
                null, // House ID
                new RobotPosition(0, 0),
                new Orientation(0),
                0,
                new BatteryState(100, false),
                RobotStatus.IDLE,
                Instant.now()
        );
    }

    @Override
    public RobotPosition getPosition() {
        return getState().position();
    }

    @Override
    public Orientation getOrientation() {
        return getState().orientation();
    }

    @Override
    public BatteryState getBattery() {
        return getState().battery();
    }

    @Override
    public RobotCapabilities getCapabilities() {
        return new RobotCapabilities(
                true, true, false, 250.0, 90.0,
                Set.of("START_CLEANING", "PAUSE", "STOP", "MOVE")
        );
    }

    @Override
    public CleaningState getCleaningState() {
        return new CleaningState(null, "Dock", 0, 0, 0);
    }

    @Override
    public MotionState getMotionState() {
        return new MotionState(0, 0, 0, false);
    }

    @Override
    public DistanceSensor getDistanceSensor() {
        return distanceSensor;
    }

    @Override
    public Camera getCamera() {
        return camera;
    }

    @Override
    public IMU getImu() {
        return imu;
    }

    @Override
    public WheelEncoder getWheelEncoder() {
        return wheelEncoder;
    }

    @Override
    public BatterySensor getBatterySensor() {
        return batterySensor;
    }

    @Override
    public CliffSensor getCliffSensor() {
        return cliffSensor;
    }

    @Override
    public RobotPosition getEstimatedPosition() {
        return getOrInitSlam().getEstimatedPose().position();
    }

    @Override
    public Orientation getEstimatedOrientation() {
        return getOrInitSlam().getEstimatedPose().orientation();
    }

    @Override
    public OccupancyGrid getSlamMap() {
        return getOrInitSlam().getMap();
    }

    @Override
    public void updateSlam() {
        getOrInitSlam().process(
            wheelEncoder.read(),
            imu.read(),
            distanceSensor.read()
        );
    }

    private SlamEngine getOrInitSlam() {
        return slamEngine.updateAndGet(current -> {
            if (current != null) return current;
            return slamFactory.create(100, 100, 100.0);
        });
    }

    @Override
    public void move(MotionCommand command) {
        log.debug("PhysicalRobot.move() - sending to WebSocket: {}", command);
        String payload = "{\"linear\": %f, \"angular\": %f, \"duration\": %d}".formatted(
                command.linearVelocity(), command.angularVelocity(), command.duration().toMillis());
        communicationHandler.sendToRobot(robotId, new RobotCommandMessage(
                RobotCommandMessage.TYPE, robotId, "MOVE", payload, Instant.now()));
    }

    @Override
    public void stop() {
        log.info("PhysicalRobot.stop()");
        communicationHandler.sendToRobot(robotId, new RobotCommandMessage(
                RobotCommandMessage.TYPE, robotId, "STOP", null, Instant.now()));
    }

    @Override
    public void pause() {
        log.info("PhysicalRobot.pause()");
        communicationHandler.sendToRobot(robotId, new RobotCommandMessage(
                RobotCommandMessage.TYPE, robotId, "PAUSE", null, Instant.now()));
    }

    @Override
    public void resume() {
        log.info("PhysicalRobot.resume()");
        communicationHandler.sendToRobot(robotId, new RobotCommandMessage(
                RobotCommandMessage.TYPE, robotId, "RESUME", null, Instant.now()));
    }

    @Override
    public void navigateTo(RobotPosition goal) {
        log.info("PhysicalRobot.navigateTo({}) - sending as high-level intent", goal);
        String payload = "{\"x\": %f, \"y\": %f}".formatted(goal.x(), goal.y());
        communicationHandler.sendToRobot(robotId, new RobotCommandMessage(
                RobotCommandMessage.TYPE, robotId, "NAVIGATE", payload, Instant.now()));
    }

    @Override
    public RobotCommandResult executeCommand(String type, String payload) {
        log.info("PhysicalRobot.executeCommand() - type={}, payload={}", type, payload);
        communicationHandler.sendToRobot(robotId, new RobotCommandMessage(
                RobotCommandMessage.TYPE, robotId, type, payload, Instant.now()));
        return new RobotCommandResult(UUID.randomUUID(), "ACCEPTED", Instant.now());
    }
}
