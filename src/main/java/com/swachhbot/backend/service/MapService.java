package com.swachhbot.backend.service;

import com.swachhbot.backend.domain.House;
import com.swachhbot.backend.domain.OccupancyMap;
import com.swachhbot.backend.dto.MapDtos.MapDto;
import com.swachhbot.backend.dto.MapDtos.MapUpdateRequest;
import com.swachhbot.backend.learning.MapSnapshotService;
import com.swachhbot.backend.repository.HouseRepository;
import com.swachhbot.backend.repository.OccupancyMapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MapService {

    private final OccupancyMapRepository mapRepository;
    private final HouseRepository houseRepository;
    private final MapSnapshotService snapshotService;

    @Transactional(readOnly = true)
    public MapDto getLatest(UUID houseId) {
        OccupancyMap map = mapRepository.findFirstByHouseIdOrderByUpdatedAtDesc(houseId)
                .orElseThrow(() -> new ResourceNotFoundException("No map for house: " + houseId));
        return toDto(map);
    }

    /** Full replace of the map for a house (client pushes its local grid). */
    public MapDto save(UUID houseId, MapUpdateRequest request) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new ResourceNotFoundException("House not found: " + houseId));

        OccupancyMap map = mapRepository.findFirstByHouseIdOrderByUpdatedAtDesc(houseId)
                .orElseGet(() -> OccupancyMap.builder().house(house).build());

        map.setGridWidth(request.gridWidth());
        map.setGridHeight(request.gridHeight());
        map.setCellSize(request.cellSize());
        map.setMapData(request.mapData());
        map.setVersion(map.getVersion() + 1);

        OccupancyMap saved = mapRepository.save(map);

        // Keep a historical copy so the learning layer can detect changes over time.
        snapshotService.capture(houseId, saved.getGridWidth(), saved.getGridHeight(),
                saved.getCellSize(), saved.getMapData());

        return toDto(saved);
    }

    private MapDto toDto(OccupancyMap map) {
        return new MapDto(
                map.getId(),
                map.getHouse().getId(),
                map.getGridWidth(),
                map.getGridHeight(),
                map.getCellSize(),
                map.getMapData(),
                map.getVersion(),
                map.getUpdatedAt()
        );
    }
}
