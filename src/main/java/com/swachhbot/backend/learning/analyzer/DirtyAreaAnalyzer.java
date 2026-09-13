package com.swachhbot.backend.learning.analyzer;

import com.swachhbot.backend.domain.ProblemArea;
import com.swachhbot.backend.domain.Room;
import com.swachhbot.backend.domain.learning.InsightCategory;
import com.swachhbot.backend.learning.InsightAnalyzer;
import com.swachhbot.backend.repository.ProblemAreaRepository;
import com.swachhbot.backend.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Finds rooms that repeatedly need extra attention.
 *
 * <p>Evidence = the accumulated frequency of dirt-related problem reports whose
 * coordinates fall inside the room.
 */
@Component
@RequiredArgsConstructor
public class DirtyAreaAnalyzer implements InsightAnalyzer {

    private static final List<String> DIRT_KEYWORDS =
            List.of("dirt", "dust", "stain", "spill", "mess", "crumb", "litter", "sticky");

    /** Rooms with at least this much accumulated evidence are reported. */
    private static final int MIN_EVIDENCE = 2;

    private final RoomRepository roomRepository;
    private final ProblemAreaRepository problemAreaRepository;

    @Override
    public List<InsightDraft> analyze(UUID houseId) {
        List<Room> rooms = roomRepository.findByHouseId(houseId);
        List<ProblemArea> problems = problemAreaRepository.findByHouseIdOrderByFrequencyDesc(houseId);
        List<InsightDraft> drafts = new ArrayList<>();

        for (Room room : rooms) {
            int evidence = 0;
            for (ProblemArea problem : problems) {
                if (!inside(room, problem)) {
                    continue;
                }
                if (matchesDirt(problem.getDescription())) {
                    evidence += problem.getFrequency();
                }
            }
            if (evidence >= MIN_EVIDENCE) {
                drafts.add(new InsightDraft(
                        InsightCategory.DIRTY_AREA,
                        "ROOM:" + room.getName().toUpperCase(Locale.ROOT),
                        room.getName(),
                        "The %s repeatedly needs extra cleaning (flagged %d time(s))."
                                .formatted(room.getName(), evidence),
                        evidence,
                        "{\"roomName\":\"%s\"}".formatted(room.getName())
                ));
            }
        }
        return drafts;
    }

    private boolean inside(Room room, ProblemArea p) {
        return p.getX() >= room.getX() && p.getX() <= room.getX() + room.getWidth()
                && p.getY() >= room.getY() && p.getY() <= room.getY() + room.getHeight();
    }

    private boolean matchesDirt(String description) {
        if (description == null) {
            return false;
        }
        String d = description.toLowerCase(Locale.ROOT);
        return DIRT_KEYWORDS.stream().anyMatch(d::contains);
    }
}
