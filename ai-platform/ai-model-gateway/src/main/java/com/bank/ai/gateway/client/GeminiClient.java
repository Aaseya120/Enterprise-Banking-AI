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
 * Calls the Gemini generateContent API. Falls back to a deterministic demo
 * response when no API key is configured.
 */
@SuppressWarnings("null")
public class GeminiClient extends AbstractAiModelClient {

    private final WebClient webClient;
    private final AiProviderProperties.Provider config;

    public GeminiClient(WebClient.Builder webClientBuilder, AiProviderProperties properties) {
        this.config = properties.getGemini();
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl()
                : "https://generativelanguage.googleapis.com/v1beta";
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public ModelProvider provider() {
        return ModelProvider.GEMINI;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected AiResponse callProvider(AiRequest request, String assembledPrompt) {
        if (!config.isEnabled() || config.getApiKey() == null) {
            return DemoResponses.forProvider(ModelProvider.GEMINI, "gemini-2.5-pro (demo)", assembledPrompt);
        }
        try {
            String model = config.getModel() != null ? config.getModel() : "gemini-2.5-pro";
            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(Map.of("text", assembledPrompt))
                    ))
            );

            Map<String, Object> resp = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/models/" + model + ":generateContent")
                            .queryParam("key", config.getApiKey())
                            .build())
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) resp.get("candidates");
            Map<String, Object> contentObj = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) contentObj.get("parts");
            String text = (String) parts.get(0).get("text");

            return new AiResponse(text, ModelProvider.GEMINI, model, List.of(), 0, 0, 0L, true);
        } catch (Exception e) {
            throw AiPlatformException.providerError("Gemini call failed", e);
        }
    }
}
