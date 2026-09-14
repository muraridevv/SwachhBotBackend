package com.swachhbot.backend.assistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the conversational assistant.
 *
 * <h2>Tool-calling architecture</h2>
 * <pre>
 *  Android chat UI
 *        │  POST /api/assistant/chat
 *        ▼
 *  AssistantService ──▶ ChatClient ──▶ OpenRouter
 *        │                    │
 *        │                    ├─ read-only tools  (getHouseMap, getRoomInformation,
 *        │                    │   getCleaningHistory, getRobotStatus, getLearnedKnowledge…)
 *        │                    │      → execute immediately, they cannot change anything
 *        │                    │
 *        │                    └─ command tools    (createCleaningPlan, startCleaning,
 *        │                        pauseCleaning, stopCleaning)
 *        │                           → DO NOT move the robot.
 *        │                             They write a PENDING row and return "confirm me".
 *        ▼
 *  AssistantResponse { reply, reasoning[], pendingActions[] }
 *        │
 *        ▼
 *  User taps Confirm ──▶ POST /api/assistant/actions/{id}/confirm
 *        │
 *        ▼
 *  AssistantActionService ──▶ PlanExecutionService (re-validates!) ──▶ CommandService
 *        │
 *        ▼
 *  CleaningCommand ──▶ deterministic navigation engine ──▶ motors
 * </pre>
 *
 * The model is therefore never on the path that moves hardware: it can only
 * produce a proposal, and proposals expire unless a human confirms them.
 */
@Configuration
public class AssistantConfig {

    public static final String SYSTEM_PROMPT = """
            You are SwachhBot, a friendly floor-cleaning robot assistant talking to the person
            who owns the house.

            You help them understand the house and prepare cleaning runs.

            HOW YOU WORK
            - Use the tools you are given to look things up before answering. Never invent house
              facts, room names, battery levels or history.
            - If a tool returns no data, say so plainly instead of guessing.

            ACTIONS AND SAFETY (very important)
            - You cannot move the robot yourself. Tools like startCleaning, pauseCleaning and
              stopCleaning only PREPARE an action for the user to confirm.
            - NEVER say you have started, paused or stopped cleaning. Say you have prepared it and
              that they need to confirm.
            - If the user asks for something you cannot do, say so.

            STYLE
            - Be brief and concrete. Usually one to three short sentences.
            - Prefer plain numbers and room names over jargon.
            - When you propose an action, tell them exactly what it will do.
            """;

    /** Recent turns kept per conversation so follow-ups like "and the bedroom?" work. */
    @Bean
    public ChatMemory assistantChatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(24)
                .build();
    }

    @Bean
    public MessageChatMemoryAdvisor assistantMemoryAdvisor(ChatMemory assistantChatMemory) {
        return MessageChatMemoryAdvisor.builder(assistantChatMemory).build();
    }

    @Bean
    public ChatClient assistantChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}
