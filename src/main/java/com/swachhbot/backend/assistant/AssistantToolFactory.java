package com.swachhbot.backend.assistant;

import com.swachhbot.backend.ai.planner.AiPlanningService;
import com.swachhbot.backend.assistant.tools.HouseQueryTools;
import com.swachhbot.backend.assistant.tools.RobotCommandTools;
import com.swachhbot.backend.learning.LearningService;
import com.swachhbot.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Builds the tool objects for a single chat turn.
 *
 * <p>The tools are instantiated per request so that {@code houseId} and
 * {@code robotId} are <b>bound by the server</b>. The model can never ask about
 * or act on a different house, no matter what the prompt says.
 */
@Component
@RequiredArgsConstructor
public class AssistantToolFactory {

    private final HouseRepository houseRepository;
    private final RoomRepository roomRepository;
    private final RobotObjectRepository objectRepository;
    private final CleaningSessionRepository sessionRepository;
    private final ProblemAreaRepository problemAreaRepository;
    private final OccupancyMapRepository mapRepository;
    private final RobotStateRepository robotStateRepository;
    private final AssistantActionRepository actionRepository;
    private final LearningService learningService;
    private final AiPlanningService planningService;

    public HouseQueryTools queryTools(UUID houseId, String robotId, ActionProposalCollector collector) {
        return new HouseQueryTools(
                houseId, robotId, collector,
                houseRepository, roomRepository, objectRepository, sessionRepository,
                problemAreaRepository, mapRepository, robotStateRepository, learningService);
    }

    public RobotCommandTools commandTools(UUID houseId, String robotId, String conversationId,
                                          ActionProposalCollector collector) {
        return new RobotCommandTools(
                houseId, robotId, conversationId, collector, actionRepository, planningService);
    }
}
