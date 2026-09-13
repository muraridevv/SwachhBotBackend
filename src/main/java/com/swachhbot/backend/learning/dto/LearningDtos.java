package com.swachhbot.backend.learning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Payloads for the "🧠 What the Robot Learned" screen. */
public final class LearningDtos {

    private LearningDtos() {
    }

    public record InsightDto(
            UUID id,
            String category,
            String subjectKey,
            String subjectLabel,
            String summary,
            double confidence,
            int evidenceCount,
            String status,
            String source,
            String details,
            Instant lastUpdatedAt
    ) {
    }

    public record CorrectionRequest(
            @NotNull UUID houseId,
            @NotBlank String targetType,   // OBJECT | ROOM | AREA
            @NotBlank String targetKey,    // e.g. "chair"
            @NotBlank String assertion     // e.g. "That chair is temporary."
    ) {
    }

    public record CorrectionResultDto(
            boolean applied,
            String message,
            List<String> affected
    ) {
    }

    public record RefreshSummaryDto(
            int created,
            int reinforced,
            int decayed,
            int skipped,
            int observed
    ) {
    }

    /** Everything the UI needs in one round trip. */
    public record LearningOverview(
            UUID houseId,
            String headline,
            List<InsightDto> insights,
            List<RecommendationDto> recommendations,
            List<InsightDto> recentChanges,
            int confirmedCount,
            int emergingCount,
            Instant generatedAt
    ) {
    }

    public record RecommendationDto(
            String category,
            String subject,
            String message,
            String suggestedChange,
            double confidence,
            String status
    ) {
    }
}
