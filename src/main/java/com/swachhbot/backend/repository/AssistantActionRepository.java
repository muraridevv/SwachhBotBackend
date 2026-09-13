package com.swachhbot.backend.repository;

import com.swachhbot.backend.domain.assistant.AssistantActionEntity;
import com.swachhbot.backend.domain.assistant.AssistantActionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssistantActionRepository extends JpaRepository<AssistantActionEntity, UUID> {

    List<AssistantActionEntity> findByConversationIdAndStatusOrderByCreatedAtDesc(
            String conversationId, AssistantActionStatus status);

    List<AssistantActionEntity> findByHouseIdOrderByCreatedAtDesc(UUID houseId);
}
