package com.swachhbot.backend.learning;

import com.swachhbot.backend.domain.learning.InsightSource;
import com.swachhbot.backend.domain.learning.InsightStatus;
import com.swachhbot.backend.domain.learning.LearnedInsightEntity;
import com.swachhbot.backend.repository.LearnedInsightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Runs every {@link InsightAnalyzer} and reconciles the results with stored
 * knowledge.
 *
 * <h2>Rules that keep learning trustworthy</h2>
 * <ul>
 *   <li><b>Reinforcement</b> — repeated evidence raises confidence on a saturating curve.</li>
 *   <li><b>Decay</b> — a derived insight that stops appearing loses evidence and is
 *       eventually forgotten, so the robot can unlearn stale habits.</li>
 *   <li><b>Human authority</b> — {@code USER} insights always win;
 *       {@code USER_REJECTED} insights are never resurrected by the analysers.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LearningService {

    private final List<InsightAnalyzer> analyzers;
    private final LearnedInsightRepository insightRepository;
    private final ConfidenceModel confidenceModel;

    /** Re-derives all knowledge for a house. Idempotent. */
    @Transactional
    public Summary refresh(UUID houseId) {
        List<InsightAnalyzer.InsightDraft> drafts = analyzers.stream()
                .flatMap(a -> a.analyze(houseId).stream())
                .toList();

        Map<String, InsightAnalyzer.InsightDraft> seen = new LinkedHashMap<>();
        drafts.forEach(d -> seen.put(key(d.category().name(), d.subjectKey()), d));

        List<LearnedInsightEntity> existing =
                insightRepository.findByHouseIdOrderByConfidenceDesc(houseId);
        Map<String, LearnedInsightEntity> existingByKey = new HashMap<>();
        existing.forEach(e -> existingByKey.put(key(e.getCategory().name(), e.getSubjectKey()), e));

        int created = 0;
        int reinforced = 0;
        int skipped = 0;

        for (InsightAnalyzer.InsightDraft draft : drafts) {
            String k = key(draft.category().name(), draft.subjectKey());
            LearnedInsightEntity entity = existingByKey.get(k);

            if (entity == null) {
                insightRepository.save(newEntity(houseId, draft));
                created++;
                continue;
            }

            // A human rejected this assumption: never let the analysers bring it back.
            if (entity.getSource() == InsightSource.USER && entity.getStatus() == InsightStatus.USER_REJECTED) {
                skipped++;
                continue;
            }

            if (entity.getSource() == InsightSource.USER && entity.getStatus() == InsightStatus.USER_CONFIRMED) {
                // Keep the human's confidence, but refresh the wording/evidence.
                entity.setSummary(draft.summary());
                entity.setEvidenceCount(Math.max(entity.getEvidenceCount(), draft.evidenceCount()));
                entity.setLastUpdatedAt(Instant.now());
                insightRepository.save(entity);
                skipped++;
                continue;
            }

            entity.setSummary(draft.summary());
            entity.setDetails(draft.details());
            entity.setSubjectLabel(draft.subjectLabel());
            entity.setEvidenceCount(draft.evidenceCount());
            entity.setConfidence(confidenceModel.confidenceFor(draft.evidenceCount()));
            entity.setStatus(confidenceModel.statusFor(draft.evidenceCount()));
            entity.setLastUpdatedAt(Instant.now());
            insightRepository.save(entity);
            reinforced++;
        }

        // Decay anything derived that we did not observe this run.
        int decayed = 0;
        for (LearnedInsightEntity entity : existing) {
            if (entity.getSource() == InsightSource.USER) {
                continue;
            }
            if (seen.containsKey(key(entity.getCategory().name(), entity.getSubjectKey()))) {
                continue;
            }
            int evidence = entity.getEvidenceCount() - 1;
            if (evidence <= 0) {
                insightRepository.delete(entity);
            } else {
                entity.setEvidenceCount(evidence);
                entity.setConfidence(confidenceModel.confidenceFor(evidence));
                entity.setLastUpdatedAt(Instant.now());
                insightRepository.save(entity);
            }
            decayed++;
        }

        log.info("Learning refresh for house {}: {} created, {} reinforced, {} decayed, {} skipped",
                houseId, created, reinforced, decayed, skipped);
        return new Summary(created, reinforced, decayed, skipped, drafts.size());
    }

    @Transactional(readOnly = true)
    public List<LearnedInsightEntity> getInsights(UUID houseId) {
        return insightRepository.findByHouseIdOrderByConfidenceDesc(houseId);
    }

    @Transactional(readOnly = true)
    public List<LearnedInsightEntity> getActionable(UUID houseId) {
        return insightRepository.findByHouseIdOrderByConfidenceDesc(houseId).stream()
                .filter(i -> i.getStatus() != InsightStatus.USER_REJECTED)
                .filter(i -> confidenceModel.isActionable(i.getConfidence()))
                .toList();
    }

    private LearnedInsightEntity newEntity(UUID houseId, InsightAnalyzer.InsightDraft draft) {
        Instant now = Instant.now();
        return LearnedInsightEntity.builder()
                .id(UUID.randomUUID())
                .houseId(houseId)
                .category(draft.category())
                .subjectKey(draft.subjectKey())
                .subjectLabel(draft.subjectLabel())
                .summary(draft.summary())
                .confidence(confidenceModel.confidenceFor(draft.evidenceCount()))
                .evidenceCount(draft.evidenceCount())
                .status(confidenceModel.statusFor(draft.evidenceCount()))
                .source(InsightSource.DERIVED)
                .details(draft.details())
                .firstLearnedAt(now)
                .lastUpdatedAt(now)
                .build();
    }

    private String key(String category, String subjectKey) {
        return category + "|" + subjectKey;
    }

    /** Result of a refresh, surfaced to the API. */
    public record Summary(
            int created,
            int reinforced,
            int decayed,
            int skipped,
            int observed
    ) {
    }
}
