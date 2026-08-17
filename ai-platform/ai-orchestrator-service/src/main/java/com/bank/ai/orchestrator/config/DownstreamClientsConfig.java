package com.bank.ai.orchestrator.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class DownstreamClientsConfig {

    @Bean
    @Qualifier("ragServiceClient")
    public WebClient ragServiceClient(
            @Value("${bank.services.rag-service.base-url:http://localhost:8082}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    @Qualifier("mcpGatewayClient")
    public WebClient mcpGatewayClient(
            @Value("${bank.services.mcp-gateway-service.base-url:http://localhost:8083}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}
