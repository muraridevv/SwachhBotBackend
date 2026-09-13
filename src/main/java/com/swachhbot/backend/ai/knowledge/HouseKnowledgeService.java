package com.swachhbot.backend.ai.knowledge;

import com.swachhbot.backend.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Retrieval side of the RAG pipeline.
 *
 * <p>Given a natural-language request, returns the most semantically relevant
 * slices of house memory (layout, objects, cleaning history, problem areas,
 * past plans) so the LLM reasons over facts rather than guessing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HouseKnowledgeService {

    private final VectorStore vectorStore;
    private final AiProperties properties;

    /**
     * @return the top-K documents relevant to {@code query}, scoped to one house.
     */
    public List<Document> retrieve(UUID houseId, String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(properties.getTopK())
                .filterExpression("houseId == '" + houseId + "'")
                .build();

        try {
            return vectorStore.similaritySearch(request);
        } catch (Exception e) {
            // RAG is an enhancement, never a hard dependency: degrade to no context.
            log.warn("Semantic retrieval failed, continuing without context", e);
            return List.of();
        }
    }

    /** Formats retrieved documents into a compact block for the prompt. */
    public String buildContext(UUID houseId, String query) {
        List<Document> docs = retrieve(houseId, query);
        if (docs.isEmpty()) {
            return "(no relevant house memory found)";
        }
        return docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));
    }
}
