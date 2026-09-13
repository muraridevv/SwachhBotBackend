package com.swachhbot.backend.repository;

import com.swachhbot.backend.domain.learning.InsightCategory;
import com.swachhbot.backend.domain.learning.LearnedInsightEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearnedInsightRepository extends JpaRepository<LearnedInsightEntity, UUID> {

    List<LearnedInsightEntity> findByHouseIdOrderByConfidenceDesc(UUID houseId);

    List<LearnedInsightEntity> findByHouseIdAndCategory(UUID houseId, InsightCategory category);

    Optional<LearnedInsightEntity> findByHouseIdAndCategoryAndSubjectKey(
            UUID houseId, InsightCategory category, String subjectKey);
}
