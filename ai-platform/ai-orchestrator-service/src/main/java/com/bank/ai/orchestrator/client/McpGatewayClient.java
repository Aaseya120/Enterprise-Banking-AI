package com.bank.ai.orchestrator.client;

import com.bank.common.exception.AiPlatformException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Set;

@Component
public class McpGatewayClient {

    private final WebClient webClient;

    public McpGatewayClient(@Qualifier("mcpGatewayClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> invokeTool(String toolName, Map<String, Object> arguments,
                                           String callerId, Set<String> callerRoles) {
        try {
            Map<String, Object> body = Map.of(
                    "toolName", toolName,
                    "arguments", arguments,
                    "callerId", callerId,
                    "callerRoles", callerRoles
            );
            return webClient.post()
                    .uri("/api/v1/mcp/invoke")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            throw AiPlatformException.toolInvocationError("mcp-gateway invoke call failed", e);
        }
    }
}
