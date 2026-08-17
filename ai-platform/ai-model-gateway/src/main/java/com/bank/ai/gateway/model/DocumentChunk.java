package com.bank.ai.gateway.model;

import java.util.Map;

/**
 * A single retrieved chunk of context, as produced by rag-service's
 * VectorStore search and passed to AiModelClient.generateWithContext.
 */
public record DocumentChunk(
        String chunkId,
        String documentId,
        String text,
        double score,
        Map<String, Object> metadata
) {
}
