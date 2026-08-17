package com.bank.ai.rag.vectorstore;

import java.util.List;
import java.util.Map;

/**
 * Provider-independent vector store contract. Concrete adapters
 * (PineconeVectorStore, WeaviateVectorStore, MilvusVectorStore,
 * ChromaVectorStore) all implement this so rag-service is never coupled to
 * one vendor (architecture plan section 12). This scaffold ships an
 * in-memory implementation (InMemoryVectorStore) as the active bean; swap
 * the bean in VectorStoreConfig to point at a real provider.
 */
public interface VectorStore {

    void save(DocumentChunkRecord chunk);

    List<SearchResult> search(float[] queryEmbedding, int topK, Map<String, Object> metadataFilter);

    void delete(String documentId);

    record DocumentChunkRecord(String chunkId, String documentId, String text, float[] embedding,
                                Map<String, Object> metadata) {
    }

    record SearchResult(DocumentChunkRecord chunk, double score) {
    }
}
