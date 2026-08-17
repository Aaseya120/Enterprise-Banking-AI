package com.bank.ai.rag.service;

import com.bank.ai.rag.model.IngestDocumentRequest;
import com.bank.ai.rag.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simplified version of the ingestion pipeline in section 14. Real pipelines
 * add virus scanning, object storage, OCR, and cleaning steps before this;
 * this service picks up at "chunking" through "store in vector DB" since
 * those steps are what RAG retrieval actually depends on.
 */
@Service
public class DocumentIngestionService {

    private static final int CHUNK_SIZE_CHARS = 800;
    private static final int CHUNK_OVERLAP_CHARS = 100;

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;

    public DocumentIngestionService(EmbeddingService embeddingService, VectorStore vectorStore) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    public int ingest(IngestDocumentRequest request) {
        List<String> chunks = chunk(request.fullText());
        int index = 0;
        for (String chunkText : chunks) {
            String chunkId = request.documentId() + "-" + index;
            Map<String, Object> metadata = new HashMap<>(
                    request.metadata() != null ? request.metadata() : Map.of());
            metadata.put("documentType", request.documentType());
            metadata.put("title", request.title());
            metadata.put("documentId", request.documentId());

            vectorStore.save(new VectorStore.DocumentChunkRecord(
                    chunkId, request.documentId(), chunkText,
                    embeddingService.embed(chunkText), metadata));
            index++;
        }
        return chunks.size();
    }

    public void delete(String documentId) {
        vectorStore.delete(documentId);
    }

    private List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE_CHARS, text.length());
            chunks.add(text.substring(start, end));
            if (end == text.length()) {
                break;
            }
            start = end - CHUNK_OVERLAP_CHARS;
        }
        return chunks;
    }
}
