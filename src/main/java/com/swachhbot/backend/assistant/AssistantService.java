package com.swachhbot.backend.assistant;

import com.swachhbot.backend.assistant.dto.AssistantDtos.*;
import com.swachhbot.backend.domain.assistant.AssistantActionStatus;
import com.swachhbot.backend.repository.AssistantActionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates one turn of conversation with the assistant.
 *
 * <p>The model is given two tool sets for this turn only — read-only query tools
 * and command tools — both bound to the caller's house and robot by the server.
 * Whatever the model does, the worst outcome is a {@code PENDING} proposal.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssistantService {

    private final ChatClient assistantChatClient;
    private final MessageChatMemoryAdvisor assistantMemoryAdvisor;
    private final ChatMemory assistantChatMemory;
    private final AssistantToolFactory toolFactory;
    private final AssistantActionRepository actionRepository;
    private final AssistantActionService actionService;

    public ChatResponse chat(ChatRequest request) {
        String conversationId = (request.conversationId() == null || request.conversationId().isBlank())
                ? UUID.randomUUID().toString()
                : request.conversationId();
        String robotId = (request.robotId() == null || request.robotId().isBlank())
                ? "swachhbot-01"
                : request.robotId();

        ActionProposalCollector collector = new ActionProposalCollector();
        Object queryTools = toolFactory.queryTools(request.houseId(), robotId, collector);
        Object commandTools = toolFactory.commandTools(request.houseId(), robotId, conversationId, collector);

        String reply;
        try {
            reply = assistantChatClient.prompt()
                    .user(request.message())
                    .advisors(spec -> spec
                            .advisors(assistantMemoryAdvisor)
                            .param(ChatMemory.CONVERSATION_ID, conversationId))
                    .tools(queryTools, commandTools)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("Assistant call failed", e);
            reply = "Sorry, I could not reach my reasoning service just now. "
                    + "I can still show you what I know on the learning screen.";
        }
        if (reply == null || reply.isBlank()) {
            reply = "I did not have anything useful to say about that.";
        }

        List<ReasoningStep> reasoning = collector.notes().stream()
                .map(n -> new ReasoningStep(n.tool(), n.note()))
                .toList();

        List<ActionDto> pending = collector.proposedActionIds().stream()
                .map(actionRepository::findById)
                .flatMap(Optional::stream)
                .filter(a -> a.getStatus() == AssistantActionStatus.PENDING)
                .map(actionService::toDto)
                .toList();

        return new ChatResponse(conversationId, reply, reasoning, pending, Instant.now());
    }

    /** Clears remembered turns for a conversation. */
    public void resetConversation(String conversationId) {
        assistantChatMemory.clear(conversationId);
        log.info("Conversation {} cleared", conversationId);
    }
}
