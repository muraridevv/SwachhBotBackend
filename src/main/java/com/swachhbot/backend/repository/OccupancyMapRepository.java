package com.swachhbot.backend.repository;

import com.swachhbot.backend.domain.OccupancyMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OccupancyMapRepository extends JpaRepository<OccupancyMap, UUID> {

    Optional<OccupancyMap> findFirstByHouseIdOrderByUpdatedAtDesc(UUID houseId);
}
