package com.swachhbot.backend.learning;

import com.swachhbot.backend.domain.learning.MapSnapshotEntity;
import com.swachhbot.backend.repository.MapSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Keeps a rolling history of occupancy grids so the learning layer can diff the
 * present against the past (requirement: "compare current map with previous maps").
 */
@Service
@RequiredArgsConstructor
public class MapSnapshotService {

    /** How many snapshots to retain per house. */
    private static final int MAX_SNAPSHOTS = 20;

    private final MapSnapshotRepository snapshotRepository;

    @Transactional
    public void capture(UUID houseId, int gridWidth, int gridHeight, double cellSize, String mapData) {
        snapshotRepository.save(MapSnapshotEntity.builder()
                .id(UUID.randomUUID())
                .houseId(houseId)
                .gridWidth(gridWidth)
                .gridHeight(gridHeight)
                .cellSize(cellSize)
                .mapData(mapData)
                .capturedAt(Instant.now())
                .build());
        prune(houseId);
    }

    @Transactional(readOnly = true)
    public List<MapSnapshotEntity> history(UUID houseId) {
        return snapshotRepository.findByHouseIdOrderByCapturedAtDesc(houseId);
    }

    private void prune(UUID houseId) {
        List<MapSnapshotEntity> all = snapshotRepository.findByHouseIdOrderByCapturedAtDesc(houseId);
        if (all.size() > MAX_SNAPSHOTS) {
            snapshotRepository.deleteAll(all.subList(MAX_SNAPSHOTS, all.size()));
        }
    }
}
