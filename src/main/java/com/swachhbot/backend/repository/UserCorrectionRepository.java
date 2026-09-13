package com.swachhbot.backend.repository;

import com.swachhbot.backend.domain.learning.UserCorrectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserCorrectionRepository extends JpaRepository<UserCorrectionEntity, UUID> {

    List<UserCorrectionEntity> findByHouseIdOrderByAppliedAtDesc(UUID houseId);
}
