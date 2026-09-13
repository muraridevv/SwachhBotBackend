package com.swachhbot.backend.repository;

import com.swachhbot.backend.domain.CleaningCommand;
import com.swachhbot.backend.domain.enums.CommandStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CleaningCommandRepository extends JpaRepository<CleaningCommand, UUID> {

    List<CleaningCommand> findByRobotIdOrderByIssuedAtDesc(String robotId);

    List<CleaningCommand> findByRobotIdAndStatus(String robotId, CommandStatus status);
}
