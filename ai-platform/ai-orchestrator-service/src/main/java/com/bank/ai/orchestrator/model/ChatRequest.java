package com.bank.ai.orchestrator.model;

import java.util.Map;
import java.util.Set;

public record ChatRequest(
        String conversationId,
        String userId,
        Set<String> userRoles,
        String query,
        Map<String, Object> context
) {
}
