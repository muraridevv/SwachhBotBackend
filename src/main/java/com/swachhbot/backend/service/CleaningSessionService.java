package com.swachhbot.backend.service;

import com.swachhbot.backend.domain.CleaningSession;
import com.swachhbot.backend.domain.House;
import com.swachhbot.backend.domain.Room;
import com.swachhbot.backend.dto.SessionDtos.SessionDto;
import com.swachhbot.backend.dto.SessionDtos.SessionRequest;
import com.swachhbot.backend.intelligence.CleaningIntelligenceService;
import com.swachhbot.backend.repository.CleaningSessionRepository;
import com.swachhbot.backend.repository.HouseRepository;
import com.swachhbot.backend.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CleaningSessionService {

    private final CleaningSessionRepository sessionRepository;
    private final HouseRepository houseRepository;
    private final RoomRepository roomRepository;
    private final CleaningIntelligenceService intelligenceService;

    @Transactional(readOnly = true)
    public List<SessionDto> findByHouse(UUID houseId) {
        return sessionRepository.findByHouseIdOrderByStartedAtDesc(houseId).stream().map(this::toDto).toList();
    }

    public SessionDto create(UUID houseId, SessionRequest request) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new ResourceNotFoundException("House not found: " + houseId));

        Room room = null;
        if (request.roomId() != null) {
            room = roomRepository.findById(request.roomId()).orElse(null);
        }

        CleaningSession session = CleaningSession.builder()
                .house(house)
                .room(room)
                .startedAt(request.startedAt())
                .endedAt(request.endedAt())
                .durationSeconds(request.durationSeconds())
                .cleanedPercentage(request.cleanedPercentage())
                .areaCleanedSqm(request.areaCleanedSqm())
                .build();

        CleaningSession saved = sessionRepository.save(session);
        intelligenceService.updateStats(saved);

        return toDto(saved);
    }

    private SessionDto toDto(CleaningSession s) {
        return new SessionDto(
                s.getId(),
                s.getHouse().getId(),
                s.getRoom() != null ? s.getRoom().getId() : null,
                s.getStartedAt(),
                s.getEndedAt(),
                s.getDurationSeconds(),
                s.getCleanedPercentage(),
                s.getAreaCleanedSqm()
        );
    }
}
