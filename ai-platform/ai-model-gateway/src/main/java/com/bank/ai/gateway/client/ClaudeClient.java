package com.bank.ai.gateway.client;

import com.bank.ai.gateway.config.AiProviderProperties;
import com.bank.ai.gateway.model.AiRequest;
import com.bank.ai.gateway.model.AiResponse;
import com.bank.ai.gateway.model.ModelProvider;
import com.bank.common.exception.AiPlatformException;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Calls the Anthropic Messages API. Falls back to a deterministic demo
 * response when no API key is configured, same as OpenAiClient/GeminiClient.
 */
@SuppressWarnings("null")
public class ClaudeClient extends AbstractAiModelClient {

    private final WebClient webClient;
    private final AiProviderProperties.Provider config;

    public ClaudeClient(WebClient.Builder webClientBuilder, AiProviderProperties properties) {
        this.config = properties.getClaude();
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : "https://api.anthropic.com/v1";
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public ModelProvider provider() {
        return ModelProvider.CLAUDE;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected AiResponse callProvider(AiRequest request, String assembledPrompt) {
        if (!config.isEnabled() || config.getApiKey() == null) {
            return DemoResponses.forProvider(ModelProvider.CLAUDE, "claude-sonnet (demo)", assembledPrompt);
        }
        try {
            Map<String, Object> body = Map.of(
                    "model", config.getModel() != null ? config.getModel() : "claude-sonnet-4-6",
                    "max_tokens", request.maxTokens() != null ? request.maxTokens() : 1024,
                    "messages", List.of(Map.of("role", "user", "content", assembledPrompt))
            );

            Map<String, Object> resp = webClient.post()
                    .uri("/messages")
                    .header("x-api-key", config.getApiKey())
                    .header("anthropic-version", "2023-06-01")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map<String, Object>> content = (List<Map<String, Object>>) resp.get("content");
            String text = (String) content.get(0).get("text");
            Map<String, Object> usage = (Map<String, Object>) resp.getOrDefault("usage", Map.of());

            return new AiResponse(
                    text,
                    ModelProvider.CLAUDE,
                    (String) resp.getOrDefault("model", config.getModel()),
                    List.of(),
                    ((Number) usage.getOrDefault("input_tokens", 0)).intValue(),
                    ((Number) usage.getOrDefault("output_tokens", 0)).intValue(),
                    0L,
                    true
            );
        } catch (Exception e) {
            throw AiPlatformException.providerError("Claude call failed", e);
        }
    }
}
