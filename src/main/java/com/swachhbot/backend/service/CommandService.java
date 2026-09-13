package com.swachhbot.backend.service;

import com.swachhbot.backend.domain.CleaningCommand;
import com.swachhbot.backend.domain.House;
import com.swachhbot.backend.domain.enums.CommandStatus;
import com.swachhbot.backend.dto.RobotDtos.CommandAckRequest;
import com.swachhbot.backend.dto.RobotDtos.CommandDto;
import com.swachhbot.backend.dto.RobotDtos.CommandRequest;
import com.swachhbot.backend.repository.CleaningCommandRepository;
import com.swachhbot.backend.repository.HouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CommandService {

    private final CleaningCommandRepository commandRepository;
    private final HouseRepository houseRepository;

    @Transactional(readOnly = true)
    public List<CommandDto> findByRobot(String robotId) {
        return commandRepository.findByRobotIdOrderByIssuedAtDesc(robotId).stream().map(this::toDto).toList();
    }

    /** Issue a new command; connected robots pick it up over the command channel. */
    public CommandDto issue(CommandRequest request) {
        House house = null;
        if (request.houseId() != null) {
            house = houseRepository.findById(request.houseId()).orElse(null);
        }

        CleaningCommand command = CleaningCommand.builder()
                .robotId(request.robotId())
                .house(house)
                .command(request.command())
                .status(CommandStatus.PENDING)
                .payload(request.payload())
                .build();

        return toDto(commandRepository.save(command));
    }

    public CommandDto acknowledge(UUID id, CommandAckRequest request) {
        CleaningCommand command = commandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Command not found: " + id));
        command.setStatus(request.status());
        command.setAckedAt(Instant.now());
        return toDto(commandRepository.save(command));
    }

    private CommandDto toDto(CleaningCommand c) {
        return new CommandDto(
                c.getId(),
                c.getRobotId(),
                c.getHouse() != null ? c.getHouse().getId() : null,
                c.getCommand(),
                c.getStatus(),
                c.getPayload(),
                c.getIssuedAt(),
                c.getAckedAt()
        );
    }
}
