package com.swachhbot.backend.assistant;

import com.swachhbot.backend.ai.execution.PlanExecutionService;
import com.swachhbot.backend.assistant.dto.AssistantDtos.ActionDto;
import com.swachhbot.backend.domain.assistant.AssistantActionEntity;
import com.swachhbot.backend.domain.assistant.AssistantActionStatus;
import com.swachhbot.backend.domain.enums.CommandType;
import com.swachhbot.backend.dto.RobotDtos.CommandRequest;
import com.swachhbot.backend.repository.AssistantActionRepository;
import com.swachhbot.backend.service.CommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The <b>only</b> code path that can turn an assistant proposal into movement.
 *
 * <p>Confirmation is an explicit, user-initiated REST call. Everything is
 * re-checked here: the action must still be PENDING, must not have expired, and
 * must belong to the robot the caller is authorised for. Plans are handed to
 * {@link PlanExecutionService}, which re-validates them from the database before
 * dispatching a high-level command. The LLM has no involvement past this point.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssistantActionService {

    private final AssistantActionRepository actionRepository;
    private final PlanExecutionService planExecutionService;
    private final CommandService commandService;

    @Transactional
    public ActionDto confirm(UUID actionId, String robotId) {
        AssistantActionEntity action = load(actionId);

        if (action.getStatus() != AssistantActionStatus.PENDING) {
            throw new ActionRejectedException("This action is no longer pending (status=" + action.getStatus() + ")");
        }
        if (action.getExpiresAt().isBefore(Instant.now())) {
            action.setStatus(AssistantActionStatus.EXPIRED);
            action.setResolvedAt(Instant.now());
            actionRepository.save(action);
            throw new ActionRejectedException("This action expired. Please ask again.");
        }
        // Authorisation: a caller may only confirm actions for their own robot.
        if (robotId != null && !robotId.isBlank() && !robotId.equals(action.getRobotId())) {
            throw new ActionRejectedException("This action belongs to a different robot");
        }

        dispatch(action);

        action.setStatus(AssistantActionStatus.EXECUTED);
        action.setResolvedAt(Instant.now());
        actionRepository.save(action);
        log.info("Assistant action {} ({}) confirmed and dispatched", actionId, action.getActionType());
        return toDto(action);
    }

    @Transactional
    public ActionDto reject(UUID actionId) {
        AssistantActionEntity action = load(actionId);
        if (action.getStatus() == AssistantActionStatus.PENDING) {
            action.setStatus(AssistantActionStatus.REJECTED);
            action.setResolvedAt(Instant.now());
            actionRepository.save(action);
        }
        return toDto(action);
    }

    @Transactional(readOnly = true)
    public List<ActionDto> pending(String conversationId) {
        return actionRepository
                .findByConversationIdAndStatusOrderByCreatedAtDesc(conversationId, AssistantActionStatus.PENDING)
                .stream().map(this::toDto).toList();
    }

    // ------------------------------------------------------------- internal

    private void dispatch(AssistantActionEntity action) {
        switch (action.getActionType()) {
            case "START_CLEANING" -> {
                if (action.getPlanId() != null) {
                    // Re-validates the stored plan before issuing a high-level command.
                    planExecutionService.execute(action.getPlanId(), action.getRobotId());
                } else {
                    commandService.issue(new CommandRequest(
                            action.getRobotId(), action.getHouseId(), CommandType.START_CLEANING, null));
                }
            }
            case "PAUSE_CLEANING" -> commandService.issue(new CommandRequest(
                    action.getRobotId(), action.getHouseId(), CommandType.PAUSE, null));
            case "STOP_CLEANING" -> commandService.issue(new CommandRequest(
                    action.getRobotId(), action.getHouseId(), CommandType.STOP, null));
            default -> throw new ActionRejectedException("Unknown action type: " + action.getActionType());
        }
    }

    private AssistantActionEntity load(UUID actionId) {
        return actionRepository.findById(actionId)
                .orElseThrow(() -> new ActionRejectedException("Unknown action: " + actionId));
    }

    ActionDto toDto(AssistantActionEntity a) {
        return new ActionDto(
                a.getId(),
                a.getActionType(),
                a.getStatus().name(),
                a.getSummary(),
                a.getPlanId(),
                a.getCreatedAt(),
                a.getExpiresAt()
        );
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class ActionRejectedException extends RuntimeException {
        public ActionRejectedException(String message) {
            super(message);
        }
    }
}
