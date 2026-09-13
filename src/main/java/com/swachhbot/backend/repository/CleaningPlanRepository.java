package com.swachhbot.backend.repository;

import com.swachhbot.backend.domain.plan.CleaningPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CleaningPlanRepository extends JpaRepository<CleaningPlanEntity, UUID> {

    List<CleaningPlanEntity> findByHouseIdOrderByCreatedAtDesc(UUID houseId);
}
