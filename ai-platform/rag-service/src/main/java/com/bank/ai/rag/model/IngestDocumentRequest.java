package com.bank.ai.rag.model;


import java.util.Map;

public record IngestDocumentRequest(
        String documentId,
        String documentType,
        String title,
        String fullText,
        Map<String, Object> metadata
) {
}
