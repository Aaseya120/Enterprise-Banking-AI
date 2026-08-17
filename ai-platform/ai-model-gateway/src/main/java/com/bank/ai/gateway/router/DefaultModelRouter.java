package com.bank.ai.gateway.router;

import com.bank.ai.gateway.client.AiModelClient;
import com.bank.ai.gateway.config.AiProviderProperties;
import com.bank.ai.gateway.model.AiRequest;
import com.bank.ai.gateway.model.ModelProvider;

import java.util.Map;
import java.util.Optional;

/**
 * Simple, explainable routing policy (plan section 8):
 *  - request.metadata["taskType"] = "SIMPLE_FAQ"        -> cheapest configured model
 *  - request.metadata["taskType"] = "DOCUMENT_ANALYSIS"  -> higher-reasoning model
 *  - request.metadata["taskType"] = "HIGH_RISK"          -> the org's approved/enterprise model
 *  - otherwise                                           -> configured default
 * Falls back to bank.ai.providers.fallback-provider on provider failure.
 * Real deployments would swap this for a policy service / feature-flagged rules engine.
 */
public class DefaultModelRouter implements ModelRouter {

    private final Map<ModelProvider, AiModelClient> clients;
    private final AiProviderProperties properties;

    public DefaultModelRouter(Map<ModelProvider, AiModelClient> clients, AiProviderProperties properties) {
        this.clients = clients;
        this.properties = properties;
    }

    @Override
    public AiModelClient selectModel(AiRequest request) {
        String taskType = Optional.ofNullable(request.metadata())
                .map(m -> (String) m.get("taskType"))
                .orElse("DEFAULT");

        ModelProvider provider = switch (taskType) {
            case "SIMPLE_FAQ" -> ModelProvider.GEMINI;
            case "DOCUMENT_ANALYSIS", "FRAUD_ANALYSIS" -> ModelProvider.CLAUDE;
            case "HIGH_RISK" -> ModelProvider.valueOf(properties.getDefaultProvider());
            default -> ModelProvider.valueOf(properties.getDefaultProvider());
        };

        return clients.getOrDefault(provider, clients.get(ModelProvider.valueOf(properties.getDefaultProvider())));
    }

    @Override
    public AiModelClient fallbackModel(AiRequest request) {
        return clients.get(ModelProvider.valueOf(properties.getFallbackProvider()));
    }
}
