package com.swachhbot.backend.service;

import com.swachhbot.backend.domain.CleaningCommand;
import com.swachhbot.backend.domain.House;
import com.swachhbot.backend.domain.enums.CommandStatus;
import com.swachhbot.backend.domain.enums.RobotStatus;
import com.swachhbot.backend.dto.RobotDtos.CommandAckRequest;
import com.swachhbot.backend.dto.RobotDtos.CommandDto;
import com.swachhbot.backend.dto.RobotDtos.CommandRequest;
import com.swachhbot.backend.repository.CleaningCommandRepository;
import com.swachhbot.backend.repository.HouseRepository;
import com.swachhbot.backend.robot.RobotProperties;
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
    private final RobotStateService robotStateService;
    private final RobotProperties robotProperties;

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

        CleaningCommand saved = commandRepository.save(command);

        // The simulator runs in-process, so there is no external robot client to
        // acknowledge commands or publish telemetry. Apply high-level commands
        // immediately to make the command-center flow behave like a live robot.
        if ("simulation".equalsIgnoreCase(robotProperties.getMode())) {
            applySimulationCommand(request, house);
            saved.setStatus(CommandStatus.COMPLETED);
            saved.setAckedAt(Instant.now());
            saved = commandRepository.save(saved);
        }

        return toDto(saved);
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

    private void applySimulationCommand(CommandRequest request, House commandHouse) {
        var current = robotStateService.get(request.robotId());
        RobotStatus status = switch (request.command()) {
            case START_CLEANING, RESUME -> RobotStatus.CLEANING;
            case PAUSE -> RobotStatus.PAUSED;
            case RETURN_TO_DOCK -> RobotStatus.RETURNING;
            case STOP -> RobotStatus.IDLE;
            default -> current.status();
        };

        // A newly created simulated robot starts just inside the map rather
        // than at coordinate (0, 0), where its marker would be clipped.
        boolean uninitialized = current.houseId() == null && current.x() == 0 && current.y() == 0;
        double x = uninitialized ? 125 : current.x();
        double y = uninitialized ? 125 : current.y();
        robotStateService.update(new com.swachhbot.backend.dto.RobotDtos.RobotStateDto(
                request.robotId(),
                commandHouse != null ? commandHouse.getId() : current.houseId(),
                x, y, current.rotation(), 0, current.battery(), status,
                false, Instant.now()));
    }
}
