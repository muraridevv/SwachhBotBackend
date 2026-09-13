package com.swachhbot.backend.ai.dto;

/**
 * A natural-language explanation of a plan, produced by the LLM but derived
 * only from the already-validated {@code CleaningPlan}. Explanations are for
 * humans; they never feed back into robot behaviour.
 */
public record PlanExplanation(
        String summary,
        String reasoning
) {
}
