package com.bank.ai.rag.vectorstore;

import com.bank.common.exception.AiPlatformException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cosine-similarity search over an in-memory map. Not for production scale,
 * but keeps the exact same VectorStore contract a real PineconeVectorStore /
 * WeaviateVectorStore / MilvusVectorStore / ChromaVectorStore would use, so
 * swapping in a real provider later is a one-class change (just implement
 * VectorStore and update the @Primary bean in VectorStoreConfig).
 */
@Component
@Qualifier("inMemory")
public class InMemoryVectorStore implements VectorStore {

    private final Map<String, DocumentChunkRecord> store = new ConcurrentHashMap<>();

    @Override
    public void save(DocumentChunkRecord chunk) {
        store.put(chunk.chunkId(), chunk);
    }

    @Override
    public List<SearchResult> search(float[] queryEmbedding, int topK, Map<String, Object> metadataFilter) {
        try {
            return store.values().stream()
                    .filter(c -> matchesFilter(c, metadataFilter))
                    .map(c -> new SearchResult(c, cosineSimilarity(queryEmbedding, c.embedding())))
                    .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
                    .limit(topK)
                    .toList();
        } catch (Exception e) {
            throw AiPlatformException.vectorSearchError("In-memory vector search failed", e);
        }
    }

    @Override
    public void delete(String documentId) {
        store.values().removeIf(c -> c.documentId().equals(documentId));
    }

    private boolean matchesFilter(DocumentChunkRecord chunk, Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) {
            return true;
        }
        return filter.entrySet().stream()
                .allMatch(e -> java.util.Objects.equals(chunk.metadata().get(e.getKey()), e.getValue()));
    }

    private double cosineSimilarity(float[] a, float[] b) {
        int len = Math.min(a.length, b.length);
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < len; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
