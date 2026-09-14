package com.swachhbot.backend.robot;

import com.swachhbot.backend.dto.RobotDtos.RobotStateDto;
import com.swachhbot.backend.robot.model.BatteryState;
import com.swachhbot.backend.robot.model.SensorReading;
import com.swachhbot.backend.service.RobotStateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class RobotSensorTest {

    @Autowired
    private Robot robot;

    @MockBean
    private RobotStateService stateService;

    @Test
    void shouldProvideDistanceSensorReadings() {
        SensorReading<Map<String, Double>> reading = robot.getDistanceSensor().read();
        assertThat(reading).isNotNull();
        assertThat(reading.value()).containsKey("front");
        assertThat(reading.timestamp()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void shouldProvideBatterySensorReadings() {
        when(stateService.get(anyString())).thenReturn(new RobotStateDto(
            "swachhbot-01", null, 100.0, 100.0, 0.0, 0.0, 85.0, null, false, Instant.now()
        ));
        
        SensorReading<BatteryState> reading = robot.getBatterySensor().read();
        assertThat(reading).isNotNull();
        assertThat(reading.value().level()).isEqualTo(85.0);
    }

    @Test
    void shouldProvideImuReadings() {
        when(stateService.get(anyString())).thenReturn(new RobotStateDto(
            "swachhbot-01", null, 100.0, 100.0, 180.0, 0.0, 100.0, null, false, Instant.now()
        ));

        var reading = robot.getImu().read();
        assertThat(reading.value().orientationDeg()).isEqualTo(180.0);
    }
}
