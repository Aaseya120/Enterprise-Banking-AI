package com.bank.ai.gateway.config;

import com.bank.ai.gateway.client.AiModelClient;
import com.bank.ai.gateway.client.ClaudeClient;
import com.bank.ai.gateway.client.GeminiClient;
import com.bank.ai.gateway.client.OpenAiClient;
import com.bank.ai.gateway.model.ModelProvider;
import com.bank.ai.gateway.router.DefaultModelRouter;
import com.bank.ai.gateway.router.ModelRouter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Any service that adds ai-model-gateway as a dependency automatically gets
 * an OpenAiClient, ClaudeClient, GeminiClient and a ModelRouter bean --
 * no per-service wiring needed. This is what lets ai-orchestrator-service
 * and rag-service both depend only on ModelRouter/AiModelClient.
 */
@AutoConfiguration
@EnableConfigurationProperties(AiProviderProperties.class)
public class AiModelGatewayAutoConfiguration {

    @Bean
    public WebClient.Builder aiWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public OpenAiClient openAiClient(WebClient.Builder builder, AiProviderProperties props) {
        return new OpenAiClient(builder, props);
    }

    @Bean
    public ClaudeClient claudeClient(WebClient.Builder builder, AiProviderProperties props) {
        return new ClaudeClient(builder, props);
    }

    @Bean
    public GeminiClient geminiClient(WebClient.Builder builder, AiProviderProperties props) {
        return new GeminiClient(builder, props);
    }

    @Bean
    public ModelRouter modelRouter(OpenAiClient openAiClient, ClaudeClient claudeClient,
                                    GeminiClient geminiClient, AiProviderProperties props) {
        Map<ModelProvider, AiModelClient> clients = java.util.stream.Stream
                .of((AiModelClient) openAiClient, claudeClient, geminiClient)
                .collect(Collectors.toMap(AiModelClient::provider, c -> c));
        return new DefaultModelRouter(clients, props);
    }
}
