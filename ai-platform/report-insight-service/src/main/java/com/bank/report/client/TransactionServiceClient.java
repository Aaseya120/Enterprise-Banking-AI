package com.bank.report.client;

import com.bank.common.exception.AiPlatformException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;


import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The only place raw transaction rows exist in this service's memory --
 * MetricsAggregationService immediately reduces the list this returns down
 * to an AccountMetrics aggregate and nothing downstream (especially not
 * AiInsightService) ever sees the list itself.
 */
@Component
public class TransactionServiceClient {

    private final WebClient webClient;

    public TransactionServiceClient(WebClient transactionServiceClient) {
        this.webClient = transactionServiceClient;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRecentTransactions(String accountId, int periodDays) {
        try {
            Map<String, Object> page = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/transactions/account/{accountId}")
                            .queryParam("size", 500)
                            .build(accountId))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map<String, Object>> content = (List<Map<String, Object>>) page.getOrDefault("content", List.of());
            Instant cutoff = Instant.now().minusSeconds((long) periodDays * 86400);

            return content.stream()
                    .filter(t -> {
                        Object createdAt = t.get("createdAt");
                        return createdAt == null || Instant.parse(createdAt.toString()).isAfter(cutoff);
                    })
                    .toList();
        } catch (Exception e) {
            throw AiPlatformException.toolInvocationError("Failed to fetch transactions for " + accountId, e);
        }
    }
}
