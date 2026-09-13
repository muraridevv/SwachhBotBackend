package com.swachhbot.backend.learning;

import com.swachhbot.backend.domain.learning.InsightCategory;
import com.swachhbot.backend.domain.learning.InsightStatus;
import com.swachhbot.backend.domain.learning.LearnedInsightEntity;
import com.swachhbot.backend.learning.dto.LearningDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Adaptive-learning API backing the "🧠 What the Robot Learned" screen.
 */
@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class LearningController {

    private final LearningService learningService;
    private final RecommendationService recommendationService;
    private final CorrectionService correctionService;

    /** Re-derives knowledge from history (safe to call repeatedly). */
    @PostMapping("/analyze")
    public RefreshSummaryDto analyze(@RequestParam UUID houseId) {
        LearningService.Summary s = learningService.refresh(houseId);
        return new RefreshSummaryDto(s.created(), s.reinforced(), s.decayed(), s.skipped(), s.observed());
    }

    @GetMapping("/insights")
    public List<InsightDto> insights(@RequestParam UUID houseId) {
        return learningService.getInsights(houseId).stream().map(this::toDto).toList();
    }

    /** Single call that powers the whole screen. */
    @GetMapping("/overview")
    public LearningOverview overview(@RequestParam UUID houseId) {
        List<LearnedInsightEntity> insights = learningService.getInsights(houseId);

        List<InsightDto> insightDtos = insights.stream().map(this::toDto).toList();

        List<InsightDto> recentChanges = insights.stream()
                .filter(i -> i.getCategory() == InsightCategory.FURNITURE_CHANGE
                        || i.getCategory() == InsightCategory.MAP_CHANGE)
                .sorted(Comparator.comparing(LearnedInsightEntity::getLastUpdatedAt).reversed())
                .map(this::toDto)
                .toList();

        List<RecommendationDto> recommendations = recommendationService.forHouse(houseId).stream()
                .map(r -> new RecommendationDto(
                        r.category(), r.subject(), r.message(), r.suggestedChange(),
                        r.confidence(), r.status()))
                .toList();

        int confirmed = (int) insights.stream()
                .filter(i -> i.getStatus() == InsightStatus.CONFIRMED
                        || i.getStatus() == InsightStatus.USER_CONFIRMED)
                .count();
        int emerging = (int) insights.stream()
                .filter(i -> i.getStatus() == InsightStatus.EMERGING)
                .count();

        return new LearningOverview(
                houseId,
                headline(confirmed, emerging),
                insightDtos,
                recommendations,
                recentChanges,
                confirmed,
                emerging,
                Instant.now()
        );
    }

    /** "That chair is temporary." — updates house memory and pins the insight. */
    @PostMapping("/corrections")
    public CorrectionResultDto correct(@Valid @RequestBody CorrectionRequest request) {
        CorrectionService.CorrectionResult result = correctionService.apply(
                request.houseId(), request.targetType(), request.targetKey(), request.assertion());
        return new CorrectionResultDto(result.applied(), result.message(), result.affected());
    }

    // ---------------------------------------------------------------------

    private InsightDto toDto(LearnedInsightEntity e) {
        return new InsightDto(
                e.getId(),
                e.getCategory().name(),
                e.getSubjectKey(),
                e.getSubjectLabel(),
                e.getSummary(),
                e.getConfidence(),
                e.getEvidenceCount(),
                e.getStatus().name(),
                e.getSource().name(),
                e.getDetails(),
                e.getLastUpdatedAt()
        );
    }

    private String headline(int confirmed, int emerging) {
        if (confirmed == 0 && emerging == 0) {
            return "I am still learning about this house.";
        }
        return "I am confident about %d thing(s) and still unsure about %d.".formatted(confirmed, emerging);
    }
}
