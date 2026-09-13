package com.swachhbot.backend.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Payloads for the in-app AI assistant. */
public final class AssistantDtos {

    private AssistantDtos() {
    }

    public record ChatRequest(
            @NotNull UUID houseId,
            String robotId,
            /** Omit to start a new conversation. */
            String conversationId,
            @NotBlank String message
    ) {
    }

    public record ChatResponse(
            String conversationId,
            String reply,
            /** Honest list of what the assistant actually looked up (not model self-report). */
            List<ReasoningStep> reasoning,
            /** Actions that must be confirmed before anything moves. */
            List<ActionDto> pendingActions,
            Instant at
    ) {
    }

    public record ReasoningStep(
            String tool,
            String note
    ) {
    }

    public record ActionDto(
            UUID id,
            String actionType,
            String status,
            String summary,
            UUID planId,
            Instant createdAt,
            Instant expiresAt
    ) {
    }
}
