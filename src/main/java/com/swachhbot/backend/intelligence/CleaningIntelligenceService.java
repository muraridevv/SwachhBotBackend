package com.swachhbot.backend.intelligence;

import com.swachhbot.backend.domain.ProblemArea;
import com.swachhbot.backend.domain.Room;
import com.swachhbot.backend.domain.intelligence.RoomCleaningStats;
import com.swachhbot.backend.domain.learning.InsightCategory;
import com.swachhbot.backend.domain.learning.LearnedInsightEntity;
import com.swachhbot.backend.intelligence.config.CleaningIntelligenceProperties;
import com.swachhbot.backend.intelligence.dto.IntelligenceDtos.HouseCleaningStrategy;
import com.swachhbot.backend.intelligence.dto.IntelligenceDtos.RoomPriorityScore;
import com.swachhbot.backend.learning.support.RoomLocator;
import com.swachhbot.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CleaningIntelligenceService {

    private final RoomRepository roomRepository;
    private final LearnedInsightRepository insightRepository;
    private final ProblemAreaRepository problemAreaRepository;
    private final RoomCleaningStatsRepository statsRepository;
    private final CleaningIntelligenceProperties props;
    private final ChatClient assistantChatClient;

    /**
     * Updates per-room stats after a cleaning session.
     */
    public void updateStats(com.swachhbot.backend.domain.CleaningSession session) {
        if (session.getRoom() == null) return;

        RoomCleaningStats stats = statsRepository.findById(session.getRoom().getId())
                .orElseGet(() -> RoomCleaningStats.builder().roomId(session.getRoom().getId()).build());

        stats.setLastCleanedAt(session.getStartedAt());
        stats.setTotalCleanedCount(stats.getTotalCleanedCount() + 1);
        
        // Rolling average for coverage
        double newCoverage = ((stats.getAvgCoverage() * (stats.getTotalCleanedCount() - 1)) + session.getCleanedPercentage()) 
                / stats.getTotalCleanedCount();
        stats.setAvgCoverage(newCoverage);

        // Rolling average for duration
        long newDuration = ((stats.getAvgDurationSec() * (stats.getTotalCleanedCount() - 1)) + session.getDurationSeconds())
                / stats.getTotalCleanedCount();
        stats.setAvgDurationSec(newDuration);

        statsRepository.save(stats);
        log.info("Updated cleaning stats for room {}", session.getRoom().getName());
    }

    /**
     * Ranks rooms in a house by cleaning priority using deterministic scoring.
     */
    public HouseCleaningStrategy getStrategy(UUID houseId) {
        List<Room> rooms = roomRepository.findByHouseId(houseId);
        List<LearnedInsightEntity> insights = insightRepository.findByHouseIdAndCategory(houseId, InsightCategory.DIRTY_AREA);
        List<ProblemArea> problems = problemAreaRepository.findByHouseIdOrderByFrequencyDesc(houseId);

        List<RoomPriorityScore> scores = rooms.stream()
                .map(room -> calculateScore(room, insights, problems))
                .sorted(Comparator.comparingDouble(RoomPriorityScore::totalScore).reversed())
                .toList();

        String summary = generateAiSummary(houseId, scores);

        return new HouseCleaningStrategy(houseId, scores, summary);
    }

    private RoomPriorityScore calculateScore(Room room, List<LearnedInsightEntity> allInsights, List<ProblemArea> allProblems) {
        // 1. Dirt Score: Based on evidence count of DIRTY_AREA insights in this room
        double dirtVal = allInsights.stream()
                .filter(i -> RoomLocator.roomAt(List.of(room), 
                    extractX(i.getSubjectKey()), extractY(i.getSubjectKey())).isPresent())
                .mapToDouble(LearnedInsightEntity::getEvidenceCount)
                .sum();
        double normalizedDirt = Math.min(dirtVal / 10.0, 1.0) * props.getBaseScore();

        // 2. Recency Score: Days since last cleaning
        RoomCleaningStats stats = statsRepository.findById(room.getId())
                .orElseGet(() -> RoomCleaningStats.builder().roomId(room.getId()).build());
        
        double daysSince = stats.getLastCleanedAt() == null ? 7.0 : 
                Duration.between(stats.getLastCleanedAt(), Instant.now()).toDays();
        double normalizedRecency = Math.min(daysSince / 7.0, 1.0) * props.getBaseScore();

        // 3. User Priority
        double userPriorityScore = (room.getUserPriority() / 5.0) * props.getBaseScore();

        // 4. Obstacle Penalty
        double obstaclePenalty = allProblems.stream()
                .filter(p -> RoomLocator.roomAt(List.of(room), p.getX(), p.getY()).isPresent())
                .mapToDouble(ProblemArea::getFrequency)
                .sum();
        double normalizedObstacle = Math.min(obstaclePenalty / 5.0, 1.0) * props.getBaseScore();

        double totalScore = (props.getDirtWeight() * normalizedDirt)
                + (props.getRecencyWeight() * normalizedRecency)
                + (props.getUserPriorityWeight() * userPriorityScore)
                - (props.getObstacleImpactWeight() * normalizedObstacle);

        String explanation = "Dirt: %.1f, Recency: %.1f, Priority: %d, Obstacles: %.0f"
                .formatted(dirtVal, daysSince, room.getUserPriority(), obstaclePenalty);

        return new RoomPriorityScore(
                room.getId(),
                room.getName(),
                Math.max(totalScore, 0.0),
                normalizedDirt,
                normalizedRecency,
                room.getUserPriority(),
                explanation
        );
    }

    private String generateAiSummary(UUID houseId, List<RoomPriorityScore> scores) {
        if (scores.isEmpty()) return "No rooms defined.";
        
        RoomPriorityScore top = scores.get(0);
        String factList = scores.stream()
                .map(s -> "- %s: Score %.1f (%s)".formatted(s.roomName(), s.totalScore(), s.explanation()))
                .collect(Collectors.joining("\n"));

        try {
            return assistantChatClient.prompt()
                    .user("""
                        Based on the following room scores, explain which room should be cleaned next and why. 
                        Keep it brief and conversational for the homeowner.
                        
                        %s
                        """.formatted(factList))
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("AI summary failed", e);
            return "Based on my data, the %s should be cleaned next.".formatted(top.roomName());
        }
    }

    private double extractX(String subjectKey) {
        try {
            return Double.parseDouble(subjectKey.split(":")[1]);
        } catch (Exception e) { return 0; }
    }

    private double extractY(String subjectKey) {
        try {
            return Double.parseDouble(subjectKey.split(":")[2]);
        } catch (Exception e) { return 0; }
    }
}
