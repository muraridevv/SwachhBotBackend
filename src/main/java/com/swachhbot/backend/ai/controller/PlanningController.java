package com.swachhbot.backend.ai.controller;

import com.swachhbot.backend.ai.config.AiProperties;
import com.swachhbot.backend.ai.dto.PlanExplanation;
import com.swachhbot.backend.ai.execution.PlanExecutionService;
import com.swachhbot.backend.ai.knowledge.KnowledgeIngestionService;
import com.swachhbot.backend.ai.planner.AiPlanningService;
import com.swachhbot.backend.domain.plan.CleaningPlan;
import com.swachhbot.backend.dto.RobotDtos.CommandDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Natural-language planning API.
 *
 * <p>Nothing here can move the robot directly. The flow is always:
 * text → plan → validate → (explicit execute) → high-level command.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class PlanningController {

    private final AiPlanningService planningService;
    private final PlanExecutionService executionService;
    private final KnowledgeIngestionService ingestionService;
    private final AiProperties properties;

    // ---------------------------------------------------------------------
    // Planning
    // ---------------------------------------------------------------------

    /** "Clean the kitchen." → validated cleaning plan + explanation. */
    @PostMapping("/plan")
    public PlanResponse plan(@RequestBody PlanRequest request) {
        CleaningPlan plan = planningService.plan(request.houseId(), request.request());
        PlanExplanation explanation = planningService.explain(plan);
        return new PlanResponse(plan, explanation);
    }

    // ---------------------------------------------------------------------
    // Execution (explicit second step — never automatic)
    // ---------------------------------------------------------------------

    @PostMapping("/plans/{planId}/execute")
    public ResponseEntity<CommandDto> execute(@PathVariable UUID planId,
                                              @RequestParam(required = false) String robotId) {
        String target = (robotId == null || robotId.isBlank())
                ? properties.getDefaultRobotId() : robotId;
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(executionService.execute(planId, target));
    }

    @PostMapping("/plans/{planId}/complete")
    public ResponseEntity<Void> complete(@PathVariable UUID planId) {
        executionService.complete(planId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/plans/{planId}/reject")
    public ResponseEntity<Void> reject(@PathVariable UUID planId) {
        executionService.reject(planId);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------------
    // Knowledge (RAG index)
    // ---------------------------------------------------------------------

    /** Rebuilds the pgvector knowledge base for a house. */
    @PostMapping("/knowledge/reindex")
    public ResponseEntity<ReindexResponse> reindex(@RequestParam UUID houseId) {
        int count = ingestionService.reindex(houseId);
        return ResponseEntity.ok(new ReindexResponse(houseId, count));
    }

    // ---------------------------------------------------------------------
    // Request / response payloads
    // ---------------------------------------------------------------------

    public record PlanRequest(
            @NotNull UUID houseId,
            @NotBlank String request
    ) {
    }

    public record PlanResponse(
            CleaningPlan plan,
            PlanExplanation explanation
    ) {
    }

    public record ReindexResponse(
            UUID houseId,
            int documentsIndexed
    ) {
    }
}
