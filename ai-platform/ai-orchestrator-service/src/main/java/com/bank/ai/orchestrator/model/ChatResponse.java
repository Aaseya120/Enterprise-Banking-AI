package com.bank.ai.orchestrator.model;

import java.util.List;

public record ChatResponse(
        String conversationId,
        String answer,
        Intent intent,
        List<String> sources,
        boolean grounded,
        String modelProvider
) {
}
