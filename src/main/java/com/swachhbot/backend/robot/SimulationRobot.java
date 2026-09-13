package com.swachhbot.backend.robot;

import com.swachhbot.backend.domain.enums.CommandType;
import com.swachhbot.backend.dto.RobotDtos.CommandDto;
import com.swachhbot.backend.dto.RobotDtos.CommandRequest;
import com.swachhbot.backend.dto.RobotDtos.RobotStateDto;
import com.swachhbot.backend.robot.model.*;
import com.swachhbot.backend.robot.sensors.*;
import com.swachhbot.backend.robot.sensors.simulation.*;
import com.swachhbot.backend.robot.slam.SlamEngine;
import com.swachhbot.backend.robot.slam.SlamEngineFactory;
import com.swachhbot.backend.robot.slam.model.Pose;
import com.swachhbot.backend.robot.navigation.model.OccupancyGrid;
import com.swachhbot.backend.service.CommandService;
import com.swachhbot.backend.service.RobotStateService;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Digital Twin for the Android-based simulator.
 */
@Slf4j
public class SimulationRobot implements Robot {

    private final String robotId;
    private final RobotStateService stateService;
    private final CommandService commandService;
    private final SlamEngineFactory slamFactory;

    // Sensors
    private final DistanceSensor distanceSensor;
    private final Camera camera;
    private final IMU imu;
    private final WheelEncoder wheelEncoder;
    private final BatterySensor batterySensor;
    private final CliffSensor cliffSensor;

    // SLAM
    private final AtomicReference<SlamEngine> slamEngine = new AtomicReference<>();

    public SimulationRobot(String robotId, RobotStateService stateService, CommandService commandService, SlamEngineFactory slamFactory) {
        this.robotId = robotId;
        this.stateService = stateService;
        this.commandService = commandService;
        this.slamFactory = slamFactory;
        
        // Initialize simulated sensors
        this.distanceSensor = new SimulatedDistanceSensor(robotId, stateService);
        this.camera = new SimulatedCamera(robotId);
        this.imu = new SimulatedIMU(robotId, stateService);
        this.wheelEncoder = new SimulatedWheelEncoder(robotId);
        this.batterySensor = new SimulatedBatterySensor(robotId, stateService);
        this.cliffSensor = new SimulatedCliffSensor(robotId);
    }

    @Override
    public String getRobotId() {
        return robotId;
    }

    @Override
    public RobotState getState() {
        RobotStateDto dto = stateService.get(robotId);
        return new RobotState(
                dto.robotId(),
                dto.houseId(),
                new RobotPosition(dto.x(), dto.y()),
                new Orientation(dto.rotation()),
                dto.velocity(),
                new BatteryState(dto.battery(), false), // Simulator doesn't report charging yet
                dto.status(),
                dto.updatedAt()
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
                true, true, true, 500.0, 180.0,
                Set.of("START_CLEANING", "PAUSE", "STOP", "MOVE")
        );
    }

    @Override
    public CleaningState getCleaningState() {
        // This would require more metadata from the simulator
        return new CleaningState(null, "Living Room", 0, 0, 0);
    }

    @Override
    public MotionState getMotionState() {
        RobotState s = getState();
        return new MotionState(s.velocity(), 0, 0, false);
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
        SlamEngine engine = getOrInitSlam();
        return engine.getEstimatedPose().position();
    }

    @Override
    public Orientation getEstimatedOrientation() {
        SlamEngine engine = getOrInitSlam();
        return engine.getEstimatedPose().orientation();
    }

    @Override
    public OccupancyGrid getSlamMap() {
        SlamEngine engine = getOrInitSlam();
        return engine.getMap();
    }

    @Override
    public void updateSlam() {
        SlamEngine engine = getOrInitSlam();
        engine.process(
            wheelEncoder.read(),
            imu.read(),
            distanceSensor.read()
        );
    }

    private SlamEngine getOrInitSlam() {
        return slamEngine.updateAndGet(current -> {
            if (current != null) return current;
            // Default grid for initial SLAM state
            return slamFactory.create(100, 100, 100.0); // 10m x 10m
        });
    }

    @Override
    public void move(MotionCommand command) {
        log.info("Simulation move: {} for {}ms", command, command.duration().toMillis());
        // For simulation, we wrap the motion command in a payload for the MOVE high-level command
        commandService.issue(new CommandRequest(
                robotId, null, CommandType.MOVE,
                "{\"linear\": %f, \"angular\": %f, \"duration\": %d}".formatted(
                        command.linearVelocity(), command.angularVelocity(), command.duration().toMillis())
        ));
    }

    @Override
    public void stop() {
        commandService.issue(new CommandRequest(robotId, null, CommandType.STOP, null));
    }

    @Override
    public void pause() {
        commandService.issue(new CommandRequest(robotId, null, CommandType.PAUSE, null));
    }

    @Override
    public void resume() {
        commandService.issue(new CommandRequest(robotId, null, CommandType.START_CLEANING, "{\"resume\": true}"));
    }

    @Override
    public void navigateTo(RobotPosition goal) {
        // For simulation, the NavigationEngine loop handles this. 
        // This is a no-op here as the Engine is driving the Robot.
    }

    @Override
    public RobotCommandResult executeCommand(String type, String payload) {
        CommandDto dto = commandService.issue(new CommandRequest(robotId, null, CommandType.valueOf(type), payload));
        return new RobotCommandResult(dto.id(), dto.status().name(), dto.issuedAt());
    }
}
