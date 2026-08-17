package com.bank.ai.gateway.client;

import com.bank.ai.gateway.model.AiResponse;
import com.bank.ai.gateway.model.ModelProvider;

import java.util.List;

/**
 * Lets the whole platform (orchestrator, RAG, MCP, fraud, etc.) run and be
 * demoed end-to-end with zero external API keys. Replace by simply setting
 * bank.ai.providers.<provider>.enabled=true and supplying a real key.
 */
final class DemoResponses {

    private DemoResponses() {
    }

    static AiResponse forProvider(ModelProvider provider, String modelName, String assembledPrompt) {
        String content = "[DEMO MODE - no " + provider + " API key configured] "
                + "Echoing back a summarized view of the assembled prompt so the "
                + "orchestration flow can be exercised end-to-end:\n\n"
                + truncate(assembledPrompt, 400);
        return new AiResponse(content, provider, modelName, List.of(), 0, 0, 5L, false);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
