package com.bank.report.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class DownstreamClientsConfig {

    @Bean
    public WebClient transactionServiceClient(
            @Value("${bank.services.transaction-service.base-url:http://localhost:8088}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}
