package com.swachhbot.backend.ai.planner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swachhbot.backend.ai.config.AiProperties;
import com.swachhbot.backend.ai.dto.PlanExplanation;
import com.swachhbot.backend.ai.dto.StructuredCommand;
import com.swachhbot.backend.ai.knowledge.HouseKnowledgeService;
import com.swachhbot.backend.domain.Room;
import com.swachhbot.backend.domain.plan.CleaningPlan;
import com.swachhbot.backend.domain.plan.CleaningPlanEntity;
import com.swachhbot.backend.repository.CleaningPlanRepository;
import com.swachhbot.backend.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestrates the AI planning pipeline.
 *
 * <pre>
 *   natural language
 *        │
 *        ├─(RAG)──▶ relevant house memory from pgvector
 *        │
 *        ▼
 *   ChatClient ──▶ StructuredCommand      (LLM, high level only)
 *        │  (on failure)
 *        └────────▶ RuleBasedPlanner      (deterministic fallback)
 *        │
 *        ▼
 *   PlanBuilder        ── deterministic resolution against the DB
 *        │
 *        ▼
 *   PlanValidator      ── safety gate (bounds + referential integrity)
 *        │
 *        ▼
 *   CleaningPlan (VALIDATED)  ──▶ only this may be executed
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiPlanningService {

    private final ChatClient planningChatClient;
    private final HouseKnowledgeService knowledgeService;
    private final RuleBasedPlanner ruleBasedPlanner;
    private final PlanBuilder planBuilder;
    private final PlanValidator planValidator;
    private final RoomRepository roomRepository;
    private final CleaningPlanRepository planRepository;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * Full pipeline: text → structured intent → concrete plan → validation.
     *
     * @throws PlanValidationException if the resulting plan fails any safety rule
     */
    @Transactional
    public CleaningPlan plan(UUID houseId, String naturalLanguage) {
        List<Room> rooms = roomRepository.findByHouseId(houseId);
        if (rooms.isEmpty()) {
            throw new PlanValidationException("House has no rooms to plan for");
        }
        Set<UUID> knownRoomIds = rooms.stream().map(Room::getId).collect(Collectors.toSet());

        // 1. Intent ---------------------------------------------------------
        StructuredCommand command;
        boolean aiGenerated;
        if (properties.isEnabled()) {
            String context = knowledgeService.buildContext(houseId, naturalLanguage);
            command = askLlm(naturalLanguage, context, rooms);
            aiGenerated = true;
        } else {
            command = ruleBasedPlanner.parse(naturalLanguage, rooms);
            aiGenerated = false;
        }

        // 2. Deterministic resolution ---------------------------------------
        CleaningPlan draft = planBuilder.build(houseId, naturalLanguage, command, aiGenerated);

        // 3. Safety gate -----------------------------------------------------
        planValidator.validate(draft, knownRoomIds);
        CleaningPlan validated = draft.withStatus(CleaningPlan.Status.VALIDATED);
        log.info("Validated plan {} for house {} -> {} room(s), priority {}",
                validated.planId(), houseId, validated.rooms().size(), validated.priority());

        // 4. Audit -----------------------------------------------------------
        persist(validated);
        return validated;
    }

    /** Marks a plan as rejected (e.g. the user declined it) and stores the audit row. */
    @Transactional
    public void reject(CleaningPlan plan) {
        persist(plan.withStatus(CleaningPlan.Status.REJECTED));
    }

    /** Human-readable explanation derived strictly from the already-validated plan. */
    public PlanExplanation explain(CleaningPlan plan) {
        String factSheet = """
                Action: %s
                Priority: %s
                Passes: %d
                Rooms (in order): %s
                Excluded: %s
                Estimated duration: %d seconds
                Stored reason: %s
                """.formatted(
                plan.action(), plan.priority(), plan.passes(),
                plan.rooms().stream()
                        .filter(CleaningPlan.PlannedRoom::cleanRequired)
                        .map(r -> r.order() + ". " + r.name() + " (" + r.rationale() + ")")
                        .collect(Collectors.joining("; ")),
                plan.excludedAreas(), plan.estimatedDurationSeconds(), plan.reason());

        try {
            return planningChatClient.prompt()
                    .user("""
                            Explain this cleaning plan to a non-technical homeowner in at most
                            three sentences. Do not invent facts beyond the plan below.
                            %s
                            """.formatted(factSheet))
                    .call()
                    .entity(PlanExplanation.class);
        } catch (Exception e) {
            log.warn("Explanation generation failed, returning plan's own reason", e);
            return new PlanExplanation("Cleaning plan for " + plan.rooms().size() + " room(s).", plan.reason());
        }
    }

    // ----- internals ---------------------------------------------------------

    private StructuredCommand askLlm(String request, String context, List<Room> rooms) {
        String roomList = rooms.stream().map(Room::getName).collect(Collectors.joining(", "));
        String userMessage = """
                Known rooms in this house: %s

                Relevant house memory:
                %s

                User request: %s
                """.formatted(roomList, context, request);
        try {
            StructuredCommand command = planningChatClient.prompt()
                    .user(userMessage)
                    .call()
                    .entity(StructuredCommand.class);
            if (command != null) {
                return command;
            }
            log.warn("LLM returned no structured command, falling back to rules");
        } catch (Exception e) {
            log.warn("LLM planning failed ({}), falling back to deterministic rules", e.getMessage());
        }
        return ruleBasedPlanner.parse(request, rooms);
    }

    private void persist(CleaningPlan plan) {
        try {
            CleaningPlanEntity entity = CleaningPlanEntity.builder()
                    .id(plan.planId())
                    .houseId(plan.houseId())
                    .naturalLanguage(plan.naturalLanguage())
                    .action(plan.action().name())
                    .priority(plan.priority().name())
                    .passes(plan.passes())
                    .excludedAreas(String.join(",", plan.excludedAreas()))
                    .roomsJson(objectMapper.writeValueAsString(plan.rooms()))
                    .reason(plan.reason())
                    .estimatedSeconds(plan.estimatedDurationSeconds())
                    .status(plan.status().name())
                    .aiGenerated(plan.aiGenerated())
                    .createdAt(plan.createdAt() != null ? plan.createdAt() : Instant.now())
                    .build();
            planRepository.save(entity);
        } catch (Exception e) {
            log.warn("Could not persist plan audit row", e);
        }
    }
}
