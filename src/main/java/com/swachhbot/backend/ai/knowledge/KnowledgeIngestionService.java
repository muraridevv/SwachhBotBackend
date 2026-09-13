package com.swachhbot.backend.ai.knowledge;

import com.swachhbot.backend.domain.*;
import com.swachhbot.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Write side of the RAG pipeline.
 *
 * <p>Flattens relational house memory into natural-language documents and stores
 * their embeddings in pgvector, so the planner can semantically retrieve
 * "everything about the kitchen" or "where the robot gets stuck".
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeIngestionService {

    private final VectorStore vectorStore;
    private final HouseRepository houseRepository;
    private final RobotObjectRepository objectRepository;
    private final CleaningSessionRepository sessionRepository;
    private final ProblemAreaRepository problemAreaRepository;

    /**
     * Rebuilds the searchable knowledge base for one house.
     * Idempotent: existing documents for the house are removed first.
     */
    @Transactional(readOnly = true)
    public int reindex(UUID houseId) {
        House house = houseRepository.findWithRoomsById(houseId)
                .orElseThrow(() -> new IllegalArgumentException("House not found: " + houseId));

        // 1. Drop stale documents for this house.
        try {
            FilterExpressionBuilder filters = new FilterExpressionBuilder();
            vectorStore.delete(filters.eq("houseId", houseId.toString()).build());
        } catch (Exception e) {
            log.warn("Could not purge previous knowledge documents; they may duplicate", e);
        }

        // 2. Build fresh documents.
        List<Document> documents = new ArrayList<>();
        documents.add(houseDoc(house));
        house.getRooms().forEach(room -> documents.add(roomDoc(house, room)));

        objectRepository.findByHouseId(houseId)
                .forEach(obj -> documents.add(objectDoc(house, obj)));

        sessionRepository.findByHouseIdOrderByStartedAtDesc(houseId)
                .forEach(session -> documents.add(sessionDoc(house, session)));

        problemAreaRepository.findByHouseIdOrderByFrequencyDesc(houseId)
                .forEach(problem -> documents.add(problemDoc(house, problem)));

        // 3. Store embeddings.
        vectorStore.add(documents);
        log.info("Indexed {} knowledge documents for house {}", documents.size(), houseId);
        return documents.size();
    }

    // ----- Document builders -----

    private Document houseDoc(House house) {
        String text = "House '%s' spans %.0f x %.0f units and has %d rooms: %s.".formatted(
                house.getName(), house.getWidth(), house.getHeight(), house.getRooms().size(),
                house.getRooms().stream().map(Room::getName).collect(Collectors.joining(", ")));
        return doc(text, house.getId(), "HOUSE");
    }

    private Document roomDoc(House house, Room room) {
        String furniture = room.getFurniture().isEmpty()
                ? "no furniture"
                : room.getFurniture().stream()
                .map(f -> f.getType().toLowerCase())
                .collect(Collectors.joining(", "));
        String text = "Room '%s' is at (%.0f, %.0f) with size %.0f x %.0f. It contains: %s.".formatted(
                room.getName(), room.getX(), room.getY(), room.getWidth(), room.getHeight(), furniture);
        return doc(text, house.getId(), "ROOM");
    }

    private Document objectDoc(House house, RobotObject obj) {
        String text = "Object '%s' of category %s has status %s. Last seen at (%.0f, %.0f) with %.0f%% confidence, detected %d times."
                .formatted(obj.getType(), obj.getCategory(), obj.getStatus(),
                        obj.getX(), obj.getY(), obj.getConfidence() * 100, obj.getDetectionCount());
        return doc(text, house.getId(), "OBJECT");
    }

    private Document sessionDoc(House house, CleaningSession session) {
        String text = "Cleaning session started %s, lasted %d seconds and covered %.1f%% of the floor."
                .formatted(session.getStartedAt(), session.getDurationSeconds(), session.getCleanedPercentage());
        return doc(text, house.getId(), "SESSION");
    }

    private Document problemDoc(House house, ProblemArea problem) {
        String text = "Robot problem: %s near (%.0f, %.0f). Encountered %d time(s), last on %s."
                .formatted(problem.getDescription(), problem.getX(), problem.getY(),
                        problem.getFrequency(), problem.getLastSeen());
        return doc(text, house.getId(), "PROBLEM");
    }

    private Document doc(String text, UUID houseId, String kind) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("houseId", houseId.toString());
        metadata.put("kind", kind);
        return new Document(text, metadata);
    }
}
