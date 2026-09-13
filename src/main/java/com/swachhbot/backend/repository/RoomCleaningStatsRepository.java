package com.swachhbot.backend.repository;

import com.swachhbot.backend.domain.intelligence.RoomCleaningStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoomCleaningStatsRepository extends JpaRepository<RoomCleaningStats, UUID> {
}
