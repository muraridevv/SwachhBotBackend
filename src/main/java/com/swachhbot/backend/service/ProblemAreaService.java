package com.swachhbot.backend.service;

import com.swachhbot.backend.domain.House;
import com.swachhbot.backend.domain.ProblemArea;
import com.swachhbot.backend.dto.SessionDtos.ProblemAreaDto;
import com.swachhbot.backend.dto.SessionDtos.ProblemAreaRequest;
import com.swachhbot.backend.repository.HouseRepository;
import com.swachhbot.backend.repository.ProblemAreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProblemAreaService {

    private static final double MERGE_RADIUS = 60.0; // same spot within 60 units

    private final ProblemAreaRepository problemAreaRepository;
    private final HouseRepository houseRepository;

    @Transactional(readOnly = true)
    public List<ProblemAreaDto> findByHouse(UUID houseId) {
        return problemAreaRepository.findByHouseIdOrderByFrequencyDesc(houseId)
                .stream().map(this::toDto).toList();
    }

    /**
     * Records a problem. If a nearby problem of the same description already
     * exists, its frequency is incremented instead of inserting a duplicate.
     * This is how "robot frequently gets stuck near sofa" accumulates.
     */
    public ProblemAreaDto report(UUID houseId, ProblemAreaRequest request) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new ResourceNotFoundException("House not found: " + houseId));

        ProblemArea existing = problemAreaRepository.findByHouseIdOrderByFrequencyDesc(houseId).stream()
                .filter(p -> p.getDescription().equalsIgnoreCase(request.description()))
                .filter(p -> distance(p.getX(), p.getY(), request.x(), request.y()) <= MERGE_RADIUS)
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setFrequency(existing.getFrequency() + 1);
            existing.setLastSeen(Instant.now());
            return toDto(problemAreaRepository.save(existing));
        }

        ProblemArea area = ProblemArea.builder()
                .house(house)
                .x(request.x())
                .y(request.y())
                .radius(request.radius())
                .description(request.description())
                .frequency(1)
                .lastSeen(Instant.now())
                .build();
        return toDto(problemAreaRepository.save(area));
    }

    private double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }

    private ProblemAreaDto toDto(ProblemArea p) {
        return new ProblemAreaDto(
                p.getId(),
                p.getHouse().getId(),
                p.getX(),
                p.getY(),
                p.getRadius(),
                p.getDescription(),
                p.getFrequency(),
                p.getLastSeen()
        );
    }
}
