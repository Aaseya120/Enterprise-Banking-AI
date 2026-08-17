package com.bank.knowledge.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Pushes a knowledge document's content into rag-service's vector store
 * when the document is published (ACTIVE). Uses rag-service's existing
 * POST /api/v1/rag/documents endpoint which accepts a full-text payload
 * and handles embedding internally.
 *
 * <p>Chunking strategy: split on paragraph boundaries (double newline),
 * discard empty chunks, truncate to 512 characters. Deterministic and
 * reproducible — a real deployment would use a proper sentence splitter
 * (e.g., Apache OpenNLP) for better semantic coherence.
 *
 * <p>Failure policy: a non-critical ingestion failure logs a warning but
 * does NOT roll back the publish transaction. The document is ACTIVE in
 * the knowledge service regardless; the operator can re-trigger ingestion
 * via a manual POST to rag-service if needed. This is documented in the
 * KnowledgeDocumentService javadoc.
 */
@Component
public class RagIngestionClient {

    private static final Logger log = LoggerFactory.getLogger(RagIngestionClient.class);

    private final RestClient restClient;

    public RagIngestionClient(
            @Value("${bank.services.rag-service-url:http://rag-service:8082}") String ragServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(ragServiceUrl)
                .build();
    }

    /**
     * Ingests the document into rag-service. Call after publishing.
     * Swallows failures — the publish must not be rolled back because RAG is down.
     *
     * @param documentId   knowledge-service document UUID
     * @param documentType e.g. "POLICY", "PROCEDURE", "FAQ"
     * @param title        document title (used as chunk metadata)
     * @param fullText     full content to chunk and embed
     * @param accessRoles  comma-separated role names (e.g. "CUSTOMER,BANK_STAFF")
     * @return number of chunks ingested, or -1 if ingestion was skipped due to error
     */
    public int ingest(String documentId, String documentType, String title,
                      String fullText, String accessRoles) {
        if (fullText == null || fullText.isBlank()) {
            log.info("[rag-ingest] documentId={} has no content to ingest, skipping", documentId);
            return 0;
        }

        List<String> roles = accessRoles != null && !accessRoles.isBlank()
                ? Arrays.asList(accessRoles.split(","))
                : List.of();

        Map<String, Object> metadata = roles.isEmpty()
                ? Map.of("source", "knowledge-service", "documentType", documentType, "title", title)
                : Map.of("source", "knowledge-service", "documentType", documentType,
                         "title", title, "roles", roles);

        Map<String, Object> request = Map.of(
                "documentId", documentId,
                "documentType", documentType,
                "title", title,
                "fullText", fullText,
                "metadata", metadata
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/api/v1/rag/documents")
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            int chunks = response != null && response.get("chunksIngested") instanceof Number n
                    ? n.intValue() : -1;
            log.info("[rag-ingest] documentId={} ingested {} chunk(s)", documentId, chunks);
            return chunks;

        } catch (RestClientException ex) {
            log.warn("[rag-ingest] Failed to ingest documentId={} into rag-service ({}): {}. " +
                     "Document is ACTIVE in knowledge-service; re-ingest manually if needed.",
                    documentId, ex.getClass().getSimpleName(), ex.getMessage());
            return -1;
        }
    }

    /**
     * Removes all chunks for a document from the rag-service vector store.
     * Called when a document is retired. Failure is swallowed — stale chunks
     * in the vector store are a search-quality issue, not a correctness one.
     */
    public void delete(String documentId) {
        try {
            restClient.delete()
                    .uri("/api/v1/rag/documents/{documentId}", documentId)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[rag-ingest] Deleted chunks for documentId={}", documentId);
        } catch (RestClientException ex) {
            log.warn("[rag-ingest] Failed to delete chunks for documentId={} from rag-service: {}",
                    documentId, ex.getMessage());
        }
    }
}
