package com.bank.ai.orchestrator.client;

import com.bank.ai.gateway.model.DocumentChunk;
import com.bank.common.exception.AiPlatformException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@SuppressWarnings("null")
public class RagServiceClient {

    private final WebClient webClient;

    public RagServiceClient(@Qualifier("ragServiceClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @SuppressWarnings("unchecked")
    public List<DocumentChunk> retrieve(String query, int topK, Set<String> callerRoles) {
        try {
            Map<String, Object> body = Map.of(
                    "query", query,
                    "topK", topK,
                    "metadataFilter", Map.of(),
                    "callerRoles", callerRoles
            );
            Map<String, Object> resp = webClient.post()
                    .uri("/api/v1/rag/retrieve")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map<String, Object>> chunks = (List<Map<String, Object>>) resp.get("chunks");
            return chunks.stream()
                    .map(c -> new DocumentChunk(
                            (String) c.get("chunkId"),
                            (String) c.get("documentId"),
                            (String) c.get("text"),
                            ((Number) c.get("score")).doubleValue(),
                            Map.of()))
                    .toList();
        } catch (Exception e) {
            throw AiPlatformException.vectorSearchError("rag-service retrieve call failed", e);
        }
    }
}
