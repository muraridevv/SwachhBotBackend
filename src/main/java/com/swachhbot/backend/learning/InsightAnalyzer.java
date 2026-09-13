package com.swachhbot.backend.learning;

import com.swachhbot.backend.domain.learning.InsightCategory;

import java.util.List;
import java.util.UUID;

/**
 * A pure, deterministic analysis over historical data.
 *
 * <p>Analyzers never call an LLM and never touch navigation. They only read the
 * database and emit {@link InsightDraft}s, which {@link LearningService} turns
 * into confidence-scored, persisted knowledge.
 */
public interface InsightAnalyzer {

    List<InsightDraft> analyze(UUID houseId);

    /**
     * A raw observation before confidence scoring.
     *
     * @param category      what kind of knowledge this is
     * @param subjectKey    stable machine key, e.g. {@code ROOM:KITCHEN}
     * @param subjectLabel  human label, e.g. {@code Kitchen}
     * @param summary       human-readable sentence shown on the "What the Robot Learned" screen
     * @param evidenceCount how many independent observations support this
     * @param details       optional JSON with extra structured fields
     */
    record InsightDraft(
            InsightCategory category,
            String subjectKey,
            String subjectLabel,
            String summary,
            int evidenceCount,
            String details
    ) {
    }
}
