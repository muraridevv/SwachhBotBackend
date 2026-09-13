package com.swachhbot.backend.repository;

import com.swachhbot.backend.domain.RobotObject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RobotObjectRepository extends JpaRepository<RobotObject, UUID> {

    List<RobotObject> findByHouseId(UUID houseId);

    Optional<RobotObject> findByHouseIdAndType(UUID houseId, String type);
}
