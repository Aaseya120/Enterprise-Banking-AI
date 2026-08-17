package com.bank.ai.rag.service;

import com.bank.ai.rag.model.RagQueryRequest;
import com.bank.ai.rag.model.RagRetrievalResponse;
import com.bank.ai.rag.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

import java.util.Set;

/**
 * Implements "Retrieve" + "Security Filtering" from the RAG flow
 * (User Query -> Retrieve -> Augment Context -> AI Generate -> Response,
 * section 10-11). "Augment" and "AI Generate" happen in
 * ai-orchestrator-service, which calls this endpoint first and then passes
 * the returned chunks into AiModelClient.generateWithContext().
 */
@Service
public class RagRetrievalService {

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;

    public RagRetrievalService(EmbeddingService embeddingService, VectorStore vectorStore) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    public RagRetrievalResponse retrieve(RagQueryRequest request) {
        float[] queryEmbedding = embeddingService.embed(request.query());
        int topK = request.topK() > 0 ? request.topK() : 5;

        List<VectorStore.SearchResult> results =
                vectorStore.search(queryEmbedding, topK, request.metadataFilter());

        List<RagRetrievalResponse.RetrievedChunk> filtered = results.stream()
                .filter(r -> passesRoleFilter(r, request.callerRoles()))
                .map(r -> new RagRetrievalResponse.RetrievedChunk(
                        r.chunk().chunkId(), r.chunk().documentId(), r.chunk().text(), r.score()))
                .toList();

        return new RagRetrievalResponse(filtered);
    }

    /**
     * Document-ACL enforcement (section 11): a chunk is only returned if its
     * metadata "roles" list intersects the caller's roles, or the chunk has
     * no roles restriction at all.
     */

    private boolean passesRoleFilter(VectorStore.SearchResult result, Set<String> callerRoles) {
        Object rolesMeta = result.chunk().metadata().get("roles");
        if (!(rolesMeta instanceof List<?> allowedRoles) || allowedRoles.isEmpty()) {
            return true;
        }
        if (callerRoles == null || callerRoles.isEmpty()) {
            return false;
        }
        return allowedRoles.stream().anyMatch(callerRoles::contains);
    }
}
