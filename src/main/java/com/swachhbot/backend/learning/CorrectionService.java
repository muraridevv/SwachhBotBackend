package com.swachhbot.backend.learning;

import com.swachhbot.backend.domain.RobotObject;
import com.swachhbot.backend.domain.enums.ObjectCategory;
import com.swachhbot.backend.domain.learning.*;
import com.swachhbot.backend.repository.LearnedInsightRepository;
import com.swachhbot.backend.repository.RobotObjectRepository;
import com.swachhbot.backend.repository.UserCorrectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Applies human corrections such as <i>"That chair is temporary."</i>
 *
 * <p>A correction has two effects: it updates house memory (e.g. reclassifies
 * the object), and it pins the corresponding insight as {@code USER_CONFIRMED}
 * or {@code USER_REJECTED} so the analysers can never silently undo it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CorrectionService {

    private final UserCorrectionRepository correctionRepository;
    private final LearnedInsightRepository insightRepository;
    private final RobotObjectRepository objectRepository;

    @Transactional
    public CorrectionResult apply(UUID houseId, String targetType, String targetKey, String assertion) {
        String normalizedType = targetType == null ? "OBJECT" : targetType.trim().toUpperCase(Locale.ROOT);
        String text = assertion == null ? "" : assertion.toLowerCase(Locale.ROOT);
        List<String> affected = new ArrayList<>();

        UUID insightId = switch (normalizedType) {
            case "OBJECT" -> applyObjectCorrection(houseId, targetKey, text, affected);
            case "ROOM" -> applyRoomCorrection(houseId, targetKey, text, affected);
            default -> null;
        };

        correctionRepository.save(UserCorrectionEntity.builder()
                .id(UUID.randomUUID())
                .houseId(houseId)
                .insightId(insightId)
                .targetType(normalizedType)
                .targetKey(targetKey)
                .assertion(assertion)
                .appliedAt(Instant.now())
                .build());

        String message = affected.isEmpty()
                ? "Correction recorded (nothing else needed changing)."
                : "Updated: " + String.join(", ", affected);
        return new CorrectionResult(true, message, affected);
    }

    // ---------------------------------------------------------------------

    private UUID applyObjectCorrection(UUID houseId, String objectType, String text, List<String> affected) {
        ObjectCategory category = null;
        if (text.contains("temporary") || text.contains("moves") || text.contains("movable")) {
            category = ObjectCategory.TEMPORARY;
        } else if (text.contains("permanent") || text.contains("fixed") || text.contains("stays")) {
            category = ObjectCategory.PERMANENT;
        }

        if (category != null) {
            for (RobotObject object : objectRepository.findByHouseId(houseId)) {
                if (object.getType().equalsIgnoreCase(objectType)
                        || object.getType().toLowerCase(Locale.ROOT).contains(objectType.toLowerCase(Locale.ROOT))) {
                    object.setCategory(category);
                    objectRepository.save(object);
                    affected.add(object.getType() + " reclassified as " + category);
                }
            }
        }

        String subjectKey = "OBJECT:" + objectType.toUpperCase(Locale.ROOT);
        LearnedInsightEntity insight = upsertUserInsight(
                houseId,
                InsightCategory.TEMPORARY_OBJECT,
                subjectKey,
                capitalize(objectType),
                "The user confirmed that the %s is %s.".formatted(
                        objectType, category == null ? "a fixture of the room" : category.name().toLowerCase(Locale.ROOT)),
                InsightStatus.USER_CONFIRMED);
        return insight.getId();
    }

    private UUID applyRoomCorrection(UUID houseId, String roomName, String text, List<String> affected) {
        String subjectKey = "ROOM:" + roomName.toUpperCase(Locale.ROOT);
        Optional<LearnedInsightEntity> existing = insightRepository
                .findByHouseIdAndCategoryAndSubjectKey(houseId, InsightCategory.DIRTY_AREA, subjectKey);

        boolean negative = text.contains("not dirty") || text.contains("clean") || text.contains("doesn't")
                || text.contains("does not") || text.contains("no need");

        if (existing.isPresent()) {
            LearnedInsightEntity insight = existing.get();
            insight.setSource(InsightSource.USER);
            insight.setConfidence(negative ? 0.0 : 1.0);
            insight.setStatus(negative ? InsightStatus.USER_REJECTED : InsightStatus.USER_CONFIRMED);
            insight.setSummary("The user said the %s is %s.".formatted(
                    roomName, negative ? "not a dirty area" : "a dirty area"));
            insight.setLastUpdatedAt(Instant.now());
            insightRepository.save(insight);
            affected.add("DIRTY_AREA insight for " + roomName);
            return insight.getId();
        }

        LearnedInsightEntity insight = upsertUserInsight(
                houseId,
                InsightCategory.DIRTY_AREA,
                subjectKey,
                roomName,
                "The user marked the %s as %s.".formatted(roomName, negative ? "not dirty" : "dirty"),
                negative ? InsightStatus.USER_REJECTED : InsightStatus.USER_CONFIRMED);
        insight.setConfidence(negative ? 0.0 : 1.0);
        insightRepository.save(insight);
        return insight.getId();
    }

    private LearnedInsightEntity upsertUserInsight(UUID houseId,
                                                   InsightCategory category,
                                                   String subjectKey,
                                                   String label,
                                                   String summary,
                                                   InsightStatus status) {
        Instant now = Instant.now();
        LearnedInsightEntity insight = insightRepository
                .findByHouseIdAndCategoryAndSubjectKey(houseId, category, subjectKey)
                .orElseGet(() -> LearnedInsightEntity.builder()
                        .id(UUID.randomUUID())
                        .houseId(houseId)
                        .category(category)
                        .subjectKey(subjectKey)
                        .subjectLabel(label)
                        .evidenceCount(0)
                        .firstLearnedAt(now)
                        .build());

        insight.setSummary(summary);
        insight.setSource(InsightSource.USER);
        insight.setStatus(status);
        insight.setConfidence(status == InsightStatus.USER_REJECTED ? 0.0 : 1.0);
        insight.setLastUpdatedAt(now);
        return insightRepository.save(insight);
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "Object";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    public record CorrectionResult(boolean applied, String message, List<String> affected) {
    }
}
