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
 * Calls OpenAI's Chat Completions endpoint. When no API key is configured
 * (bank.ai.providers.openai.enabled=false, the default for this scaffold),
 * falls back to a deterministic demo response so the rest of the platform
 * (orchestrator, RAG, MCP) is runnable end-to-end without live credentials.
 */
@SuppressWarnings("null")
public class OpenAiClient extends AbstractAiModelClient {

    private final WebClient webClient;
    private final AiProviderProperties.Provider config;

    public OpenAiClient(WebClient.Builder webClientBuilder, AiProviderProperties properties) {
        this.config = properties.getOpenai();
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : "https://api.openai.com/v1";
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public ModelProvider provider() {
        return ModelProvider.OPENAI;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected AiResponse callProvider(AiRequest request, String assembledPrompt) {
        if (!config.isEnabled() || config.getApiKey() == null) {
            return DemoResponses.forProvider(ModelProvider.OPENAI, "gpt-4o (demo)", assembledPrompt);
        }
        try {
            Map<String, Object> body = Map.of(
                    "model", config.getModel() != null ? config.getModel() : "gpt-4o",
                    "temperature", request.temperature() != null ? request.temperature() : 0.2,
                    "max_tokens", request.maxTokens() != null ? request.maxTokens() : 1024,
                    "messages", List.of(Map.of("role", "user", "content", assembledPrompt))
            );

            Map<String, Object> resp = webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");
            Map<String, Object> usage = (Map<String, Object>) resp.getOrDefault("usage", Map.of());

            return new AiResponse(
                    content,
                    ModelProvider.OPENAI,
                    (String) resp.getOrDefault("model", config.getModel()),
                    List.of(),
                    ((Number) usage.getOrDefault("prompt_tokens", 0)).intValue(),
                    ((Number) usage.getOrDefault("completion_tokens", 0)).intValue(),
                    0L,
                    true
            );
        } catch (Exception e) {
            throw AiPlatformException.providerError("OpenAI call failed", e);
        }
    }
}
