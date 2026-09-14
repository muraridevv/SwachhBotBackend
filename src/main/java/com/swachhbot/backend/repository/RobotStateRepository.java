package com.swachhbot.backend.repository;

import com.swachhbot.backend.domain.RobotStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import com.swachhbot.backend.domain.enums.RobotStatus;
import java.util.UUID;

public interface RobotStateRepository extends JpaRepository<RobotStateEntity, UUID> {

    Optional<RobotStateEntity> findByRobotId(String robotId);
    List<RobotStateEntity> findByStatus(RobotStatus status);
}
