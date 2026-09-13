package com.swachhbot.backend.robot.ros2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swachhbot.backend.domain.enums.RobotStatus;
import com.swachhbot.backend.robot.Robot;
import com.swachhbot.backend.robot.model.*;
import com.swachhbot.backend.robot.navigation.model.OccupancyGrid;
import com.swachhbot.backend.robot.sensors.*;
import com.swachhbot.backend.robot.slam.SlamEngine;
import com.swachhbot.backend.robot.slam.SlamEngineFactory;
import com.swachhbot.backend.robot.ros2.RosBridgeMessages.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ROS 2 Bridge Robot (Phase 21).
 * 
 * <p>Communicates with a Raspberry Pi running rosbridge_server.
 * High-level intents are mapped to ROS 2 topics and actions.
 */
@Slf4j
public class Ros2Robot extends TextWebSocketHandler implements Robot {

    private final String robotId;
    private final String bridgeUrl;
    private final ObjectMapper objectMapper;
    private final SlamEngineFactory slamFactory;

    private WebSocketSession session;
    private final AtomicReference<RobotState> lastKnownState = new AtomicReference<>(
        new RobotState("ros2-robot", null, new RobotPosition(0,0), new Orientation(0), 0, new BatteryState(100, false), RobotStatus.IDLE, Instant.now())
    );
    private final AtomicReference<SlamEngine> slamEngine = new AtomicReference<>();

    public Ros2Robot(String robotId, String bridgeUrl, ObjectMapper objectMapper, SlamEngineFactory slamFactory) {
        this.robotId = robotId;
        this.bridgeUrl = bridgeUrl;
        this.objectMapper = objectMapper;
        this.slamFactory = slamFactory;
        connect();
    }

    private void connect() {
        try {
            StandardWebSocketClient client = new StandardWebSocketClient();
            client.execute(this, bridgeUrl).whenComplete((s, ex) -> {
                if (ex == null) {
                    this.session = s;
                    log.info("Connected to ROS 2 Bridge at {}", bridgeUrl);
                    subscribeToTopics();
                } else {
                    log.error("Failed to connect to ROS 2 Bridge: {}", ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("ROS 2 connection error", e);
        }
    }

    private void subscribeToTopics() {
        send(new Subscribe("/odom", "nav_msgs/Odometry"));
        send(new Subscribe("/battery", "sensor_msgs/BatteryState"));
        // Additional subscriptions for sensors...
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Handle incoming data from ROS 2 (localization, battery, etc.)
        // Map to RobotState
    }

    @Override
    public String getRobotId() { return robotId; }

    @Override
    public RobotState getState() { return lastKnownState.get(); }

    @Override
    public RobotPosition getPosition() { return getState().position(); }

    @Override
    public Orientation getOrientation() { return getState().orientation(); }

    @Override
    public BatteryState getBattery() { return getState().battery(); }

    @Override
    public RobotCapabilities getCapabilities() {
        return new RobotCapabilities(true, true, false, 500.0, 90.0, Set.of("ROS2", "NAV2"));
    }

    @Override
    public CleaningState getCleaningState() {
        return new CleaningState(null, "Unknown", 0, 0, 0);
    }

    @Override
    public MotionState getMotionState() {
        return new MotionState(0, 0, 0, false);
    }

    @Override
    public DistanceSensor getDistanceSensor() { return null; } // ROS handles this

    @Override
    public Camera getCamera() { return null; }

    @Override
    public IMU getImu() { return null; }

    @Override
    public WheelEncoder getWheelEncoder() { return null; }

    @Override
    public BatterySensor getBatterySensor() { return null; }

    @Override
    public CliffSensor getCliffSensor() { return null; }

    @Override
    public RobotPosition getEstimatedPosition() { return getPosition(); }

    @Override
    public Orientation getEstimatedOrientation() { return getOrientation(); }

    @Override
    public OccupancyGrid getSlamMap() {
        return null; // Should pull from /map topic
    }

    @Override
    public void updateSlam() {
        // ROS 2 handles the SLAM loop internally (e.g. SLAM Toolbox)
    }

    @Override
    public void move(MotionCommand command) {
        Twist twist = new Twist(
            new Vector3(command.linearVelocity() / 1000.0, 0, 0), // mm to m
            new Vector3(0, 0, Math.toRadians(command.angularVelocity()))
        );
        send(new Publish("/cmd_vel", twist));
    }

    @Override
    public void stop() {
        move(new MotionCommand(0, 0, Duration.ZERO));
    }

    @Override
    public void pause() {
        // ROS 2 Nav2 pause/cancel
    }

    @Override
    public void resume() {
    }

    @Override
    public void navigateTo(RobotPosition goal) {
        log.info("ROS 2 Nav2: Navigating to {}", goal);
        // Map to PoseStamped and publish to /goal_pose or use Nav2 Action Server
        PoseStamped pose = new PoseStamped(
            new Header(0, new Time(Instant.now().getEpochSecond(), 0), "map"),
            new Pose(new Point(goal.x()/1000.0, goal.y()/1000.0, 0), new Quaternion(0, 0, 0, 1))
        );
        send(new Publish("/goal_pose", pose));
    }

    @Override
    public RobotCommandResult executeCommand(String type, String payload) {
        log.info("ROS 2 execute command: {}", type);
        return new RobotCommandResult(UUID.randomUUID(), "SENT_TO_ROS", Instant.now());
    }

    private void send(Object obj) {
        if (session == null || !session.isOpen()) return;
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(obj)));
        } catch (Exception e) {
            log.error("ROS send error", e);
        }
    }
}
