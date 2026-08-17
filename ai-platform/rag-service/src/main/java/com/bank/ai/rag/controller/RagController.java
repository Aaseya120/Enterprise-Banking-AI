package com.bank.ai.rag.controller;

import com.bank.ai.rag.model.IngestDocumentRequest;
import com.bank.ai.rag.model.RagQueryRequest;
import com.bank.ai.rag.model.RagRetrievalResponse;
import com.bank.ai.rag.service.DocumentIngestionService;
import com.bank.ai.rag.service.RagRetrievalService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/rag")
public class RagController {

    private final DocumentIngestionService ingestionService;
    private final RagRetrievalService retrievalService;

    public RagController(DocumentIngestionService ingestionService, RagRetrievalService retrievalService) {
        this.ingestionService = ingestionService;
        this.retrievalService = retrievalService;
    }

    @PostMapping("/documents")
    public Map<String, Object> ingest(@RequestBody IngestDocumentRequest request) {
        int chunkCount = ingestionService.ingest(request);
        return Map.of("documentId", request.documentId(), "chunksIngested", chunkCount);
    }

    @DeleteMapping("/documents/{documentId}")
    public void delete(@PathVariable String documentId) {
        ingestionService.delete(documentId);
    }

    @PostMapping("/retrieve")
    public RagRetrievalResponse retrieve(@RequestBody RagQueryRequest request) {
        return retrievalService.retrieve(request);
    }
}
