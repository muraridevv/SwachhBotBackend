package com.swachhbot.backend.robot.slam;

import com.swachhbot.backend.robot.model.SensorReading;
import com.swachhbot.backend.robot.navigation.model.OccupancyGrid;
import com.swachhbot.backend.robot.sensors.IMU;
import com.swachhbot.backend.robot.sensors.WheelEncoder;
import com.swachhbot.backend.robot.slam.model.Pose;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleSlamTest {

    @Test
    void shouldUpdatePoseFromOdometryAndImu() {
        OccupancyGrid grid = new OccupancyGrid(10, 10, 100.0, "U".repeat(100));
        SlamEngine engine = new SimpleSlamEngine(grid);

        // Initial update
        engine.process(
            new SensorReading<>(Instant.now(), "enc", new WheelEncoder.Odometry(0, 0, 0), 1.0),
            new SensorReading<>(Instant.now(), "imu", new IMU.IMUData(0, 0, 0), 1.0),
            new SensorReading<>(Instant.now(), "dist", Map.of(), 1.0)
        );

        // Move 100mm forward (0 deg is North, which is UP in screen, but North in world)
        engine.process(
            new SensorReading<>(Instant.now(), "enc", new WheelEncoder.Odometry(1000, 1000, 100.0), 1.0),
            new SensorReading<>(Instant.now(), "imu", new IMU.IMUData(0, 0, 0), 1.0),
            new SensorReading<>(Instant.now(), "dist", Map.of(), 1.0)
        );

        Pose pose = engine.getEstimatedPose();
        assertThat(pose.position().x()).isCloseTo(0.0, Offset.offset(0.001));
        assertThat(pose.position().y()).isCloseTo(-100.0, Offset.offset(0.001));
    }

    @Test
    void shouldUpdateMapFromDistanceSensor() {
        OccupancyGrid grid = new OccupancyGrid(10, 10, 100.0, "U".repeat(100));
        SlamEngine engine = new SimpleSlamEngine(grid);

        // 1. Move to (550, 550) - Grid (5,5) - facing North
        // Rad = -PI/2
        // dx = dist * cos(-PI/2) = 0
        // dy = dist * sin(-PI/2) = -dist
        // To reach y=550 starting from 0, we need dy=550, so dist=-550? 
        // Better: Start at (0,0), move 500 forward to (0, -500).
        // Let's just test at (0,0) but keep everything in bounds.
        
        // Move to (500, 500) by "cheating" or just moving there.
        // X = 500, Y = 500
        // Angle 180 (South) -> rad = PI/2. dx = 0, dy = 500.
        engine.process(
            new SensorReading<>(Instant.now(), "enc", new WheelEncoder.Odometry(0, 0, 0), 1.0),
            new SensorReading<>(Instant.now(), "imu", new IMU.IMUData(180.0, 0, 0), 1.0),
            new SensorReading<>(Instant.now(), "dist", Map.of(), 1.0)
        );
        engine.process(
            new SensorReading<>(Instant.now(), "enc", new WheelEncoder.Odometry(5000, 5000, 500.0), 1.0),
            new SensorReading<>(Instant.now(), "imu", new IMU.IMUData(180.0, 0, 0), 1.0),
            new SensorReading<>(Instant.now(), "dist", Map.of(), 1.0)
        );
        
        // Now at (0, 500) facing South.
        // Let's look "front" (South) at 200mm.
        // Grid Y = 5. Target Y = 500 + 200 = 700 -> Grid Y = 7.
        engine.process(
            new SensorReading<>(Instant.now(), "enc", new WheelEncoder.Odometry(5000, 5000, 500.0), 1.0),
            new SensorReading<>(Instant.now(), "imu", new IMU.IMUData(180.0, 0, 0), 1.0),
            new SensorReading<>(Instant.now(), "dist", Map.of("front", 200.0), 1.0)
        );

        assertThat(grid.getCell(0, 5)).isEqualTo(OccupancyGrid.CellType.FREE);
        assertThat(grid.getCell(0, 6)).isEqualTo(OccupancyGrid.CellType.FREE);
        assertThat(grid.getCell(0, 7)).isEqualTo(OccupancyGrid.CellType.OBSTACLE);
    }
}
