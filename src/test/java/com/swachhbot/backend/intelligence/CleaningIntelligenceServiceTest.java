package com.swachhbot.backend.intelligence;

import com.swachhbot.backend.domain.House;
import com.swachhbot.backend.domain.Room;
import com.swachhbot.backend.domain.intelligence.RoomCleaningStats;
import com.swachhbot.backend.intelligence.config.CleaningIntelligenceProperties;
import com.swachhbot.backend.intelligence.dto.IntelligenceDtos.HouseCleaningStrategy;
import com.swachhbot.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.client.ChatClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class CleaningIntelligenceServiceTest {

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private LearnedInsightRepository insightRepository;
    @Mock
    private ProblemAreaRepository problemAreaRepository;
    @Mock
    private RoomCleaningStatsRepository statsRepository;
    @Mock
    private CleaningIntelligenceProperties props;
    @Mock
    private ChatClient chatClient;

    @InjectMocks
    private CleaningIntelligenceService service;

    private final UUID houseId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(props.getBaseScore()).thenReturn(10.0);
        when(props.getDirtWeight()).thenReturn(0.4);
        when(props.getRecencyWeight()).thenReturn(0.3);
        when(props.getUserPriorityWeight()).thenReturn(0.2);
        when(props.getObstacleImpactWeight()).thenReturn(0.1);
    }

    @Test
    void shouldRankRoomsByScore() {
        Room r1 = Room.builder().id(UUID.randomUUID()).name("Kitchen").x(0).y(0).width(100).height(100).userPriority(5).build();
        Room r2 = Room.builder().id(UUID.randomUUID()).name("Bedroom").x(200).y(0).width(100).height(100).userPriority(1).build();
        
        when(roomRepository.findByHouseId(houseId)).thenReturn(List.of(r1, r2));
        when(insightRepository.findByHouseIdAndCategory(any(), any())).thenReturn(Collections.emptyList());
        when(problemAreaRepository.findByHouseIdOrderByFrequencyDesc(any())).thenReturn(Collections.emptyList());

        // Kitchen was cleaned 5 days ago, Bedroom 1 day ago
        when(statsRepository.findById(r1.getId())).thenReturn(Optional.of(
            RoomCleaningStats.builder().roomId(r1.getId()).lastCleanedAt(Instant.now().minus(5, ChronoUnit.DAYS)).build()
        ));
        when(statsRepository.findById(r2.getId())).thenReturn(Optional.of(
            RoomCleaningStats.builder().roomId(r2.getId()).lastCleanedAt(Instant.now().minus(1, ChronoUnit.DAYS)).build()
        ));

        HouseCleaningStrategy strategy = service.getStrategy(houseId);

        assertThat(strategy.rankedRooms()).hasSize(2);
        assertThat(strategy.rankedRooms().get(0).roomName()).isEqualTo("Kitchen");
    }

    @Test
    void shouldHandleMissingStats() {
        Room r1 = Room.builder().id(UUID.randomUUID()).name("Kitchen").x(0).y(0).width(100).height(100).userPriority(1).build();
        when(roomRepository.findByHouseId(houseId)).thenReturn(List.of(r1));
        when(statsRepository.findById(any())).thenReturn(Optional.empty());

        HouseCleaningStrategy strategy = service.getStrategy(houseId);
        assertThat(strategy.rankedRooms().get(0).recencyScore()).isEqualTo(10.0); // Max score if never cleaned
    }

    @Test
    void shouldRespectWeights() {
        Room r1 = Room.builder().id(UUID.randomUUID()).name("Kitchen").x(0).y(0).width(100).height(100).userPriority(5).build();
        when(roomRepository.findByHouseId(houseId)).thenReturn(List.of(r1));
        
        // High user priority, but let's see if dirtWeight=0 makes it irrelevant
        when(props.getUserPriorityWeight()).thenReturn(0.0);
        when(props.getRecencyWeight()).thenReturn(1.0);
        
        // Cleaned 1 hour ago
        when(statsRepository.findById(r1.getId())).thenReturn(Optional.of(
            RoomCleaningStats.builder().roomId(r1.getId()).lastCleanedAt(Instant.now().minus(1, ChronoUnit.HOURS)).build()
        ));

        HouseCleaningStrategy strategy = service.getStrategy(houseId);
        assertThat(strategy.rankedRooms().get(0).totalScore()).isLessThan(1.0);
    }
}
