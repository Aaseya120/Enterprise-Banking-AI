package com.bank.ai.mcp.client;

import com.bank.common.exception.AiPlatformException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * The ONLY path from the AI plane to account-service. Tools call this
 * client, which calls account-service's public REST API -- never the
 * database directly (architecture plan section 16).
 */
@Component
public class AccountServiceClient {

    private final WebClient webClient;

    public AccountServiceClient(WebClient accountServiceClient) {
        this.webClient = accountServiceClient;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getBalance(String accountId) {
        try {
            return webClient.get()
                    .uri("/api/v1/accounts/{id}/balance", accountId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            throw AiPlatformException.toolInvocationError("account-service getBalance failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAccountsForCustomer(String customerId) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/v1/accounts")
                            .queryParam("customerId", customerId).build())
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();
        } catch (Exception e) {
            throw AiPlatformException.toolInvocationError("account-service getAccountsForCustomer failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAccount(String accountId) {
        try {
            return webClient.get()
                    .uri("/api/v1/accounts/{id}", accountId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            throw AiPlatformException.toolInvocationError("account-service getAccount failed", e);
        }
    }
}
