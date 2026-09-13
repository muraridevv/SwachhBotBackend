package com.swachhbot.backend.learning.analyzer;

import com.swachhbot.backend.domain.RobotObject;
import com.swachhbot.backend.domain.Room;
import com.swachhbot.backend.domain.enums.ObjectCategory;
import com.swachhbot.backend.domain.learning.InsightCategory;
import com.swachhbot.backend.learning.InsightAnalyzer;
import com.swachhbot.backend.learning.support.RoomLocator;
import com.swachhbot.backend.repository.RobotObjectRepository;
import com.swachhbot.backend.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Finds objects that keep reappearing in the same place — chairs pulled out,
 * bags dropped, shoes left by the door. These are things the robot should
 * learn to treat as obstacles that will probably move again.
 */
@Component
@RequiredArgsConstructor
public class TemporaryObjectAnalyzer implements InsightAnalyzer {

    private static final int MIN_DETECTIONS = 2;

    private final RobotObjectRepository objectRepository;
    private final RoomRepository roomRepository;

    @Override
    public List<InsightDraft> analyze(UUID houseId) {
        List<Room> rooms = roomRepository.findByHouseId(houseId);
        List<InsightDraft> drafts = new ArrayList<>();

        for (RobotObject object : objectRepository.findByHouseId(houseId)) {
            boolean isTemporary = object.getCategory() == ObjectCategory.TEMPORARY
                    || object.getCategory() == ObjectCategory.MOVING;
            if (!isTemporary || object.getDetectionCount() < MIN_DETECTIONS) {
                continue;
            }
            String where = RoomLocator.describeLocation(rooms, object.getX(), object.getY());
            String label = capitalize(object.getType());

            drafts.add(new InsightDraft(
                    InsightCategory.TEMPORARY_OBJECT,
                    "OBJECT:" + object.getType().toUpperCase(Locale.ROOT),
                    label,
                    "%s keeps appearing near %s (seen %d time(s))."
                            .formatted(label, where, object.getDetectionCount()),
                    object.getDetectionCount(),
                    "{\"x\":%.0f,\"y\":%.0f,\"category\":\"%s\"}"
                            .formatted(object.getX(), object.getY(), object.getCategory())
            ));
        }
        return drafts;
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "Object";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
