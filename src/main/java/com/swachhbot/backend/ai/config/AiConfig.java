package com.swachhbot.backend.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the Spring AI {@link ChatClient} used by the planning layer.
 *
 * <h2>Architecture &amp; safety boundary</h2>
 * <pre>
 *   User text
 *      │
 *      ▼
 *   [ ChatClient ]  ──structured output──▶  StructuredCommand   (high level only)
 *      │                                            │
 *      │  RAG: VectorStore (pgvector)               │ resolved against House DB
 *      │  house / objects / history / problems      │ (deterministic)
 *      ▼                                            ▼
 *   explanation                            CleaningPlan (validated)
 *                                                   │
 *                                                   ▼
 *                                         Deterministic execution
 *                                                   │
 *                                                   ▼
 *                                        CleaningCommand  ──▶  robot firmware
 *                                                                 (motors)
 * </pre>
 *
 * The model can only ever emit the narrow {@code StructuredCommand} schema.
 * Everything physical — coordinates, speeds, angles, navigation — is produced
 * by code, never by the LLM.
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    /**
     * The system prompt both locks the model into the schema and states the
     * safety contract in plain language.
     */
    public static final String SYSTEM_PROMPT = """
            You are the high-level cleaning planner for a floor-cleaning robot named SwachhBot.

            Your ONLY job is to translate a human request into a structured intent.
            You do NOT drive the robot. You never output motor commands, speeds,
            coordinates, angles, delays, or any low-level control values.

            Always answer with a single JSON object using exactly these fields:
              - action:        one of CLEAN, CLEAN_ALL, CLEAN_EXCEPT
              - rooms:         array of room names to clean (uppercase), may be empty
              - excludedRooms: array of room names to avoid (uppercase), may be empty
              - priority:      one of LOW, NORMAL, HIGH, URGENT
              - passes:        integer number of passes (1-3), default 1
              - timeframe:     the recency filter the user mentioned, e.g. "3 days", or null
              - reasoning:     one short sentence explaining your interpretation

            Rules:
              - "Clean the kitchen"                        -> action CLEAN, rooms ["KITCHEN"]
              - "Clean the rooms not cleaned in 3 days"    -> action CLEAN_ALL, timeframe "3 days"
              - "Don't clean the bedroom"                  -> action CLEAN_EXCEPT, excludedRooms ["BEDROOM"]
              - "Clean the dirtiest areas first"           -> action CLEAN_ALL, priority HIGH
              - Use ONLY room names that appear in the provided house context.
              - If the request is unrelated to cleaning, return action CLEAN_ALL with an
                empty rooms list and explain the mismatch in reasoning.
              - Never invent room names, numbers of passes greater than 3, or extra fields.
            """;

    @Bean
    public ChatClient planningChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}
