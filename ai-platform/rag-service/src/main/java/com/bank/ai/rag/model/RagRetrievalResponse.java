package com.bank.ai.rag.model;

import java.util.List;

public record RagRetrievalResponse(List<RetrievedChunk> chunks) {

    public record RetrievedChunk(String chunkId, String documentId, String text, double score) {
    }
}
