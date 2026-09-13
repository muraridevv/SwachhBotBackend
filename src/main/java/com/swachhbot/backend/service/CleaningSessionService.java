package com.swachhbot.backend.service;

import com.swachhbot.backend.domain.CleaningSession;
import com.swachhbot.backend.domain.House;
import com.swachhbot.backend.dto.SessionDtos.SessionDto;
import com.swachhbot.backend.dto.SessionDtos.SessionRequest;
import com.swachhbot.backend.repository.CleaningSessionRepository;
import com.swachhbot.backend.repository.HouseRepository;
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

    @Transactional(readOnly = true)
    public List<SessionDto> findByHouse(UUID houseId) {
        return sessionRepository.findByHouseIdOrderByStartedAtDesc(houseId).stream().map(this::toDto).toList();
    }

    public SessionDto create(UUID houseId, SessionRequest request) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new ResourceNotFoundException("House not found: " + houseId));

        CleaningSession session = CleaningSession.builder()
                .house(house)
                .startedAt(request.startedAt())
                .endedAt(request.endedAt())
                .durationSeconds(request.durationSeconds())
                .cleanedPercentage(request.cleanedPercentage())
                .areaCleanedSqm(request.areaCleanedSqm())
                .build();

        return toDto(sessionRepository.save(session));
    }

    private SessionDto toDto(CleaningSession s) {
        return new SessionDto(
                s.getId(),
                s.getHouse().getId(),
                s.getStartedAt(),
                s.getEndedAt(),
                s.getDurationSeconds(),
                s.getCleanedPercentage(),
                s.getAreaCleanedSqm()
        );
    }
}
