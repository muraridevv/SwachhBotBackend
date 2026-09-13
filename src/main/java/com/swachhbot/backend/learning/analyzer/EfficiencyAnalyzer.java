package com.swachhbot.backend.learning.analyzer;

import com.swachhbot.backend.domain.CleaningSession;
import com.swachhbot.backend.domain.learning.InsightCategory;
import com.swachhbot.backend.learning.InsightAnalyzer;
import com.swachhbot.backend.repository.CleaningSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Learns how efficient cleaning runs are, and whether the robot is improving.
 *
 * <p>Compares the average duration of recent sessions against older ones so the
 * planner can give more realistic time estimates over time.
 */
@Component
@RequiredArgsConstructor
public class EfficiencyAnalyzer implements InsightAnalyzer {

    private static final int MIN_SESSIONS = 4;

    private final CleaningSessionRepository sessionRepository;

    @Override
    public List<InsightDraft> analyze(UUID houseId) {
        List<CleaningSession> sessions = sessionRepository.findByHouseIdOrderByStartedAtDesc(houseId);
        if (sessions.size() < MIN_SESSIONS) {
            return List.of();
        }

        int half = sessions.size() / 2;
        double recentAvg = sessions.subList(0, half).stream()
                .mapToLong(CleaningSession::getDurationSeconds)
                .average().orElse(0);
        double olderAvg = sessions.subList(half, sessions.size()).stream()
                .mapToLong(CleaningSession::getDurationSeconds)
                .average().orElse(0);

        if (olderAvg <= 0 || recentAvg <= 0) {
            return List.of();
        }

        double deltaPercent = ((olderAvg - recentAvg) / olderAvg) * 100.0;
        double avgCoverage = sessions.stream()
                .mapToDouble(CleaningSession::getCleanedPercentage)
                .average().orElse(0);

        String summary;
        if (deltaPercent >= 5) {
            summary = "Cleaning is getting faster: recent runs are %.0f%% quicker than earlier ones."
                    .formatted(deltaPercent);
        } else if (deltaPercent <= -5) {
            summary = "Cleaning is getting slower: recent runs take %.0f%% longer than earlier ones."
                    .formatted(Math.abs(deltaPercent));
        } else {
            summary = "Cleaning duration is stable at about %.0f seconds per run."
                    .formatted(recentAvg);
        }

        return List.of(new InsightDraft(
                InsightCategory.CLEANING_PATTERN,
                "HOUSE:EFFICIENCY",
                "Cleaning efficiency",
                summary + " Average floor coverage is %.0f%%.".formatted(avgCoverage),
                sessions.size(),
                "{\"sessions\":%d,\"avgSeconds\":%.0f,\"deltaPercent\":%.1f}"
                        .formatted(sessions.size(), recentAvg, deltaPercent)
        ));
    }
}
