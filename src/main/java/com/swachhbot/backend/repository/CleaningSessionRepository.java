package com.swachhbot.backend.repository;

import com.swachhbot.backend.domain.CleaningSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CleaningSessionRepository extends JpaRepository<CleaningSession, UUID> {

    List<CleaningSession> findByHouseIdOrderByStartedAtDesc(UUID houseId);
}
