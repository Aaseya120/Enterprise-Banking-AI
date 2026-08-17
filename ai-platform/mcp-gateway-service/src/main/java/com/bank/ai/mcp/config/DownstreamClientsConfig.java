package com.bank.ai.mcp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Downstream service base URLs. In Kubernetes these resolve via the
 * service-registry/DNS (e.g. http://account-service.banking.svc.cluster.local);
 * for local/dev they default to the ports used in docker-compose / this
 * repo's individual `mvn spring-boot:run` setup.
 */
@Configuration
public class DownstreamClientsConfig {

    @Bean
    public WebClient accountServiceClient(
            @Value("${bank.services.account-service.base-url:http://localhost:8081}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}
