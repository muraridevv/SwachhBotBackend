package com.swachhbot.backend.assistant;

import com.swachhbot.backend.assistant.dto.AssistantDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Conversational assistant API.
 *
 * <p>Safety shape of this API: {@code /chat} can only ever <i>propose</i>. The
 * only endpoints that can move the robot are {@code /actions/{id}/confirm},
 * which require an explicit user gesture.
 */
@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;
    private final AssistantActionService actionService;

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return assistantService.chat(request);
    }

    /** Explicit human confirmation — the only way a proposal becomes a command. */
    @PostMapping("/actions/{actionId}/confirm")
    public ActionDto confirm(@PathVariable UUID actionId,
                             @RequestParam(required = false) String robotId) {
        return actionService.confirm(actionId, robotId);
    }

    @PostMapping("/actions/{actionId}/reject")
    public ActionDto reject(@PathVariable UUID actionId) {
        return actionService.reject(actionId);
    }

    @GetMapping("/actions")
    public List<ActionDto> pending(@RequestParam String conversationId) {
        return actionService.pending(conversationId);
    }

    @PostMapping("/conversations/{conversationId}/reset")
    public ResponseEntity<Void> reset(@PathVariable String conversationId) {
        assistantService.resetConversation(conversationId);
        return ResponseEntity.noContent().build();
    }
}
