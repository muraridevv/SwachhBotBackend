package com.swachhbot.backend.service.vision;

import com.swachhbot.backend.domain.vision.EnvironmentChange;
import com.swachhbot.backend.repository.EnvironmentChangeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnvironmentAnalysisService {

    private final EnvironmentChangeRepository changeRepository;
    private final ChatClient assistantChatClient;

    /**
     * Generates an AI-powered summary of recent environment changes.
     */
    public String explainChanges(UUID houseId) {
        List<EnvironmentChange> changes = changeRepository.findByHouseIdOrderByDetectedAtDesc(houseId).stream()
                .limit(5)
                .toList();

        if (changes.isEmpty()) {
            return "The environment remains unchanged.";
        }

        String facts = changes.stream()
                .map(EnvironmentChange::getDescription)
                .collect(Collectors.joining("\n- "));

        try {
            return assistantChatClient.prompt()
                    .user("I have detected the following changes in the environment:\n- " + facts + "\n\nPlease explain these changes naturally to the homeowner.")
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("AI change explanation failed, returning deterministic facts", e);
            return "Detected changes: " + facts;
        }
    }
}
