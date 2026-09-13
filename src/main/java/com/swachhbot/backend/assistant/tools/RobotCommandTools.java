package com.swachhbot.backend.assistant.tools;

import com.swachhbot.backend.ai.planner.AiPlanningService;
import com.swachhbot.backend.assistant.ActionProposalCollector;
import com.swachhbot.backend.domain.assistant.AssistantActionEntity;
import com.swachhbot.backend.domain.assistant.AssistantActionStatus;
import com.swachhbot.backend.domain.plan.CleaningPlan;
import com.swachhbot.backend.repository.AssistantActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mutating tools — but with a critical difference:
 *
 * <p><b>These tools never move the robot.</b> They write a {@code PENDING}
 * proposal to {@code assistant_actions} and describe it to the user. Only an
 * explicit, authenticated confirmation (a separate REST call) turns a proposal
 * into a real command, and that path re-validates everything before dispatching.
 *
 * <p>So an LLM hallucination can, at worst, produce a button the user declines.
 */
@RequiredArgsConstructor
public class RobotCommandTools {

    private static final int PROPOSAL_TTL_MINUTES = 10;

    private final UUID houseId;
    private final String robotId;
    private final String conversationId;
    private final ActionProposalCollector collector;

    private final AssistantActionRepository actionRepository;
    private final AiPlanningService planningService;

    // ----------------------------------------------------------------- plan

    @Tool(name = "createCleaningPlan",
            description = "Build a validated cleaning plan (which rooms, in what order, how many "
                    + "passes, and why). This does NOT start cleaning — it only prepares and "
                    + "returns the plan so the user can review it.")
    public String createCleaningPlan(
            @ToolParam(description = "The user's request in plain language, e.g. 'clean the kitchen'")
            String request) {
        collector.record("createCleaningPlan", "built a plan for '" + request + "'");

        try {
            CleaningPlan plan = planningService.plan(houseId, request);
            collector.setLastPlanId(plan.planId());

            String rooms = plan.rooms().stream()
                    .filter(CleaningPlan.PlannedRoom::cleanRequired)
                    .map(r -> "%d. %s".formatted(r.order(), r.name()))
                    .collect(Collectors.joining("; "));

            return "Prepared a plan (id %s, NOT started yet). Action: %s. Priority: %s. Passes: %d. "
                    .formatted(plan.planId(), plan.action(), plan.priority(), plan.passes())
                    + "Rooms in order: " + rooms + ". Estimated %.0f minutes. Reason: %s"
                    .formatted(plan.estimatedDurationSeconds() / 60.0, plan.reason());
        } catch (Exception e) {
            return "I could not build a valid plan for that request: " + e.getMessage();
        }
    }

    // -------------------------------------------------------------- commands

    @Tool(name = "startCleaning",
            description = "Propose starting a cleaning run. This does NOT start the robot — it asks "
                    + "the user to confirm first. Use the plan previously built by createCleaningPlan.")
    public String startCleaning(
            @ToolParam(description = "Short reason for the user, e.g. 'the kitchen is due'")
            String reason) {
        collector.record("startCleaning", "proposed starting a clean");
        return propose(AssistantActionEntity.builder()
                .actionType("START_CLEANING")
                .summary("Start cleaning" + (reason == null || reason.isBlank() ? "" : " — " + reason))
                .planId(collector.lastPlanId())
                .build());
    }

    @Tool(name = "pauseCleaning",
            description = "Propose pausing the current cleaning run. Does NOT pause immediately; "
                    + "the user must confirm.")
    public String pauseCleaning(
            @ToolParam(description = "Short reason for the user") String reason) {
        collector.record("pauseCleaning", "proposed pausing");
        return propose(AssistantActionEntity.builder()
                .actionType("PAUSE_CLEANING")
                .summary("Pause cleaning" + (reason == null || reason.isBlank() ? "" : " — " + reason))
                .build());
    }

    @Tool(name = "stopCleaning",
            description = "Propose stopping the current cleaning run and finishing the session. "
                    + "Does NOT stop immediately; the user must confirm.")
    public String stopCleaning(
            @ToolParam(description = "Short reason for the user") String reason) {
        collector.record("stopCleaning", "proposed stopping");
        return propose(AssistantActionEntity.builder()
                .actionType("STOP_CLEANING")
                .summary("Stop cleaning" + (reason == null || reason.isBlank() ? "" : " — " + reason))
                .build());
    }

    // ------------------------------------------------------------- internal

    private String propose(AssistantActionEntity.AssistantActionEntityBuilder builder) {
        Instant now = Instant.now();
        AssistantActionEntity action = builder
                .id(UUID.randomUUID())
                .houseId(houseId)
                .robotId(robotId)
                .conversationId(conversationId)
                .status(AssistantActionStatus.PENDING)
                .createdAt(now)
                .expiresAt(now.plus(PROPOSAL_TTL_MINUTES, ChronoUnit.MINUTES))
                .build();

        actionRepository.save(action);
        collector.recordProposal(action.getId());

        return ("I have prepared the action '%s' (id %s). It is NOT running yet — "
                + "the user must confirm it in the app. Tell them what will happen and that "
                + "confirmation is required.").formatted(action.getSummary(), action.getId());
    }
}
