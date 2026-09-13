package com.swachhbot.backend.assistant;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Per-request scratchpad shared between the assistant's tools.
 *
 * <p>It captures two things for the UI:
 * <ul>
 *   <li>which tools were actually invoked (so we can show honest, concise
 *       "here is what I checked" reasoning instead of trusting the model's
 *       self-report), and</li>
 *   <li>the ids of any robot actions the assistant <em>proposed</em> — these
 *       must be confirmed by a human before anything moves.</li>
 * </ul>
 */
public class ActionProposalCollector {

    private final List<ToolNote> notes = new CopyOnWriteArrayList<>();
    private final List<UUID> proposedActionIds = new CopyOnWriteArrayList<>();

    /** Plan created during this turn, so a start proposal can reference it. */
    private volatile UUID lastPlanId;

    public void record(String tool, String note) {
        notes.add(new ToolNote(tool, note));
    }

    public void setLastPlanId(UUID planId) {
        this.lastPlanId = planId;
    }

    public UUID lastPlanId() {
        return lastPlanId;
    }

    public void recordProposal(UUID actionId) {
        proposedActionIds.add(actionId);
    }

    public List<ToolNote> notes() {
        return List.copyOf(notes);
    }

    public List<UUID> proposedActionIds() {
        return List.copyOf(proposedActionIds);
    }

    public boolean anyToolsUsed() {
        return !notes.isEmpty();
    }

    /** A single tool invocation, shown to the user as reasoning. */
    public record ToolNote(String tool, String note) {
    }
}
