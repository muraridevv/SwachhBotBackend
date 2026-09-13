package com.swachhbot.backend.repository;

import com.swachhbot.backend.domain.learning.MapSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MapSnapshotRepository extends JpaRepository<MapSnapshotEntity, UUID> {

    List<MapSnapshotEntity> findByHouseIdOrderByCapturedAtDesc(UUID houseId);
}
