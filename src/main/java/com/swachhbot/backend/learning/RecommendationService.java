package com.swachhbot.backend.learning;

import com.swachhbot.backend.domain.learning.LearnedInsightEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Turns learned knowledge into plain-language recommendations.
 *
 * <p>Templates are deterministic (no LLM). This keeps the "why" behind a
 * recommendation auditable and means the feature works even when the AI layer is
 * switched off.
 */
@Service
public class RecommendationService {

    private final LearningService learningService;

    public RecommendationService(LearningService learningService) {
        this.learningService = learningService;
    }

    public List<Recommendation> forHouse(UUID houseId) {
        List<Recommendation> recommendations = new ArrayList<>();
        for (LearnedInsightEntity insight : learningService.getActionable(houseId)) {
            String suggested = switch (insight.getCategory()) {
                case DIRTY_AREA -> "give this room one extra pass";
                case BLOCKED_AREA -> "slow down and retry here instead of pushing through";
                case TEMPORARY_OBJECT -> "treat this object as movable and re-check it each session";
                case FURNITURE_CHANGE -> "re-survey this room before the next clean";
                case MAP_CHANGE -> "refresh the map for this room";
                case CLEANING_PATTERN -> "adjust the plan's passes and time estimate";

                case ROOM_CLEANLINESS -> "clean this room less often to save battery";
            };
            recommendations.add(new Recommendation(
                    insight.getCategory().name(),
                    insight.getSubjectLabel(),
                    insight.getSummary(),
                    suggested,
                    insight.getConfidence(),
                    insight.getStatus().name()
            ));
        }
        return recommendations;
    }

    /**
     * @param category       which kind of knowledge drove this advice
     * @param subject        the room / object / area it concerns
     * @param message        human-readable explanation
     * @param suggestedChange what the planner will actually do differently
     * @param confidence     0..1
     * @param status         EMERGING / CONFIRMED / USER_CONFIRMED
     */
    public record Recommendation(
            String category,
            String subject,
            String message,
            String suggestedChange,
            double confidence,
            String status
    ) {
    }
}
