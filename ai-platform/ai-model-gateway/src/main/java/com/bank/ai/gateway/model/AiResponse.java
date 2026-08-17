package com.bank.ai.gateway.model;

import java.util.List;

public record AiResponse(
        String content,
        ModelProvider provider,
        String modelName,
        List<Citation> citations,
        int promptTokens,
        int completionTokens,
        long latencyMs,
        boolean grounded
) {
    public record Citation(String documentId, String title, double relevance) {
    }
}
