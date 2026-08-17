package com.bank.ai.rag.model;


import java.util.Map;
import java.util.Set;

public record RagQueryRequest(
        String query,
        int topK,
        Map<String, Object> metadataFilter,
        Set<String> callerRoles
) {
}
