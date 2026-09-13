package com.swachhbot.backend.learning.analyzer;

import com.swachhbot.backend.domain.Room;
import com.swachhbot.backend.domain.learning.InsightCategory;
import com.swachhbot.backend.domain.learning.MapSnapshotEntity;
import com.swachhbot.backend.learning.InsightAnalyzer;
import com.swachhbot.backend.learning.support.RoomLocator;
import com.swachhbot.backend.repository.MapSnapshotRepository;
import com.swachhbot.backend.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Detects changes between historical occupancy maps.
 *
 * <p>Small, repeated changes are reported as {@link InsightCategory#MAP_CHANGE};
 * large persistent changes (a big blob of obstacle cells appearing or vanishing)
 * are classified as {@link InsightCategory#FURNITURE_CHANGE} — i.e. a piece of
 * furniture was probably moved.
 */
@Component
@RequiredArgsConstructor
public class MapChangeAnalyzer implements InsightAnalyzer {

    private static final int BUCKET_SIZE = 100;   // world units per reported region
    private static final int MIN_CELLS = 5;       // ignore noise
    private static final int FURNITURE_CELLS = 40; // "big object moved" threshold
    private static final int MAX_PAIRS = 4;

    private final MapSnapshotRepository snapshotRepository;
    private final RoomRepository roomRepository;

    @Override
    public List<InsightDraft> analyze(UUID houseId) {
        List<MapSnapshotEntity> snapshots = snapshotRepository.findByHouseIdOrderByCapturedAtDesc(houseId);
        if (snapshots.size() < 2) {
            return List.of();
        }

        Map<String, BucketChange> changes = new LinkedHashMap<>();
        int pairs = (int) Math.min(snapshots.size() - 1, MAX_PAIRS);

        for (int i = 0; i < pairs; i++) {
            MapSnapshotEntity newer = snapshots.get(i);
            MapSnapshotEntity older = snapshots.get(i + 1);
            if (newer.getGridWidth() != older.getGridWidth()
                    || newer.getGridHeight() != older.getGridHeight()
                    || newer.getMapData().length() != older.getMapData().length()) {
                continue;
            }
            diffSnapshot(newer, older, changes);
        }

        List<Room> rooms = roomRepository.findByHouseId(houseId);
        List<InsightDraft> drafts = new ArrayList<>();

        for (Map.Entry<String, BucketChange> entry : changes.entrySet()) {
            BucketChange change = entry.getValue();
            int changedCells = change.added() + change.removed();
            if (changedCells < MIN_CELLS) {
                continue;
            }
            String[] parts = entry.getKey().split(":");
            long bx = Long.parseLong(parts[0]);
            long by = Long.parseLong(parts[1]);
            double worldX = bx + BUCKET_SIZE / 2.0;
            double worldY = by + BUCKET_SIZE / 2.0;
            String where = RoomLocator.describeLocation(rooms, worldX, worldY);

            boolean furniture = changedCells >= FURNITURE_CELLS && change.pairCount() >= 2;
            InsightCategory category = furniture
                    ? InsightCategory.FURNITURE_CHANGE
                    : InsightCategory.MAP_CHANGE;

            String summary = furniture
                    ? "Something large has moved near %s (about %d grid cells changed, seen in %d map(s))."
                    .formatted(where, changedCells, change.pairCount())
                    : "The layout near %s keeps changing (%d grid cells, seen in %d map(s))."
                    .formatted(where, changedCells, change.pairCount());

            drafts.add(new InsightDraft(
                    category,
                    "MAP:%d:%d".formatted(bx, by),
                    where,
                    summary,
                    change.pairCount(),
                    "{\"added\":%d,\"removed\":%d}".formatted(change.added(), change.removed())
            ));
        }
        return drafts;
    }

    private void diffSnapshot(MapSnapshotEntity newer, MapSnapshotEntity older, Map<String, BucketChange> changes) {
        String newData = newer.getMapData();
        String oldData = older.getMapData();
        double cellSize = newer.getCellSize() == 0 ? 10 : newer.getCellSize();
        int width = newer.getGridWidth();

        // Only buckets touched *by this pair* count as evidence for this pair.
        Set<String> touched = new HashSet<>();

        for (int index = 0; index < newData.length(); index++) {
            char now = newData.charAt(index);
            char before = oldData.charAt(index);
            boolean nowObstacle = now == 'O';
            boolean wasObstacle = before == 'O';
            if (nowObstacle == wasObstacle) {
                continue;
            }

            int cellX = index % width;
            int cellY = index / width;
            long bx = (long) (Math.floor(cellX * cellSize / BUCKET_SIZE) * BUCKET_SIZE);
            long by = (long) (Math.floor(cellY * cellSize / BUCKET_SIZE) * BUCKET_SIZE);
            String key = bx + ":" + by;

            BucketChange change = changes.computeIfAbsent(key, k -> new BucketChange());
            if (nowObstacle) {
                change.added++;
            } else {
                change.removed++;
            }
            touched.add(key);
        }

        touched.forEach(key -> changes.get(key).markPair());
    }

    private static final class BucketChange {
        private int added;
        private int removed;
        private int pairCount;

        void markPair() {
            pairCount++;
        }

        int added() {
            return added;
        }

        int removed() {
            return removed;
        }

        int pairCount() {
            return pairCount;
        }
    }
}
