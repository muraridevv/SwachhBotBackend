package com.swachhbot.backend.repository;

import com.swachhbot.backend.domain.vision.EnvironmentChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EnvironmentChangeRepository extends JpaRepository<EnvironmentChange, UUID> {
    List<EnvironmentChange> findByHouseIdOrderByDetectedAtDesc(UUID houseId);
}
