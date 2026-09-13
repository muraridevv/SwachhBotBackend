package com.swachhbot.backend.learning.analyzer;

import com.swachhbot.backend.domain.ProblemArea;
import com.swachhbot.backend.domain.Room;
import com.swachhbot.backend.domain.learning.InsightCategory;
import com.swachhbot.backend.learning.InsightAnalyzer;
import com.swachhbot.backend.learning.support.RoomLocator;
import com.swachhbot.backend.repository.ProblemAreaRepository;
import com.swachhbot.backend.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Finds spots where the robot repeatedly gets stuck or blocked.
 *
 * <p>Produces the classic learning statement:
 * <i>"The robot frequently gets stuck near the living-room sofa."</i>
 */
@Component
@RequiredArgsConstructor
public class BlockedAreaAnalyzer implements InsightAnalyzer {

    private static final List<String> BLOCKED_KEYWORDS =
            List.of("stuck", "block", "obstacle", "jam", "trap", "wedge", "snag", "cannot pass", "climb");

    private static final int MIN_EVIDENCE = 2;
    private static final int BUCKET_SIZE = 100;

    private final RoomRepository roomRepository;
    private final ProblemAreaRepository problemAreaRepository;

    @Override
    public List<InsightDraft> analyze(UUID houseId) {
        List<Room> rooms = roomRepository.findByHouseId(houseId);
        List<ProblemArea> problems = problemAreaRepository.findByHouseIdOrderByFrequencyDesc(houseId);
        List<InsightDraft> drafts = new ArrayList<>();

        for (ProblemArea problem : problems) {
            if (!matchesBlocked(problem.getDescription())) {
                continue;
            }
            if (problem.getFrequency() < MIN_EVIDENCE) {
                continue;
            }
            long bx = RoomLocator.bucket(problem.getX(), BUCKET_SIZE);
            long by = RoomLocator.bucket(problem.getY(), BUCKET_SIZE);
            String where = RoomLocator.describeLocation(rooms, problem.getX(), problem.getY());

            drafts.add(new InsightDraft(
                    InsightCategory.BLOCKED_AREA,
                    "AREA:%d:%d".formatted(bx, by),
                    where,
                    "The robot frequently gets blocked near %s (encountered %d time(s))."
                            .formatted(where, problem.getFrequency()),
                    problem.getFrequency(),
                    "{\"x\":%.0f,\"y\":%.0f,\"reason\":\"%s\"}"
                            .formatted(problem.getX(), problem.getY(), escape(problem.getDescription()))
            ));
        }
        return drafts;
    }

    private boolean matchesBlocked(String description) {
        if (description == null) {
            return false;
        }
        String d = description.toLowerCase(Locale.ROOT);
        return BLOCKED_KEYWORDS.stream().anyMatch(d::contains);
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\"", "'");
    }
}
