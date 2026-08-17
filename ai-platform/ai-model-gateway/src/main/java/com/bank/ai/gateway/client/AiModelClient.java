package com.bank.ai.gateway.client;

import com.bank.ai.gateway.model.AiRequest;
import com.bank.ai.gateway.model.AiResponse;
import com.bank.ai.gateway.model.DocumentChunk;
import com.bank.ai.gateway.model.ModelProvider;

import java.util.List;

/**
 * The single abstraction every AI-facing service is allowed to depend on.
 * Callers never see OpenAiClient/ClaudeClient/GeminiClient directly -- they
 * inject AiModelClient (or ModelRouter, which selects one) and call
 * generate()/generateWithContext(). This buys provider independence, easier
 * testing, fallback, and centralized observability (plan section 7).
 */
public interface AiModelClient {

    ModelProvider provider();

    AiResponse generate(AiRequest request);

    AiResponse generateWithContext(AiRequest request, List<DocumentChunk> context);
}
