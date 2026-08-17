package com.bank.ai.gateway.model;

import java.util.List;
import java.util.Map;

/**
 * Provider-agnostic request passed into AiModelClient. Nothing in this type
 * is OpenAI/Claude/Gemini specific -- each client implementation maps it to
 * its own wire format.
 */
public record AiRequest(
        String conversationId,
        String userId,
        String promptId,
        String promptVersion,
        String systemPrompt,
        String userMessage,
        List<Message> history,
        Double temperature,
        Integer maxTokens,
        Map<String, Object> metadata
) {
    public record Message(String role, String content) {
    }

    public static AiRequest simple(String conversationId, String userId, String systemPrompt, String userMessage) {
        return new AiRequest(conversationId, userId, null, null, systemPrompt, userMessage,
                List.of(), 0.2, 1024, Map.of());
    }
}
