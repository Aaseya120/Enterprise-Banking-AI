package com.bank.transfer.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Synchronous fraud pre-check for transfer-service. Called before a transfer
 * is accepted into PENDING_FRAUD_REVIEW status. Fail-open: if fraud-service
 * is unreachable, the transfer proceeds (the async fraud-ai-service saga is
 * still a second line of defence).
 */
@Component
@SuppressWarnings("null")
public class FraudServiceClient {

    private static final Logger log = LoggerFactory.getLogger(FraudServiceClient.class);

    private final RestClient restClient;

    public FraudServiceClient(
            @Value("${bank.services.fraud-service-url:http://fraud-service:8093}") String fraudServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(fraudServiceUrl)
                .build();
    }

    public FraudDecision evaluate(String accountId, BigDecimal amount, String currency) {
        try {
            Map<String, Object> request = Map.of(
                    "accountId", accountId,
                    "amount", amount,
                    "currency", currency,
                    "channel", "TRANSFER"
            );
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/api/v1/fraud/evaluate")
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            String decision = response != null ? (String) response.get("decision") : "APPROVE";
            boolean approved = !"REJECT".equalsIgnoreCase(decision);
            String reason = response != null ? extractReason(response) : "";
            return new FraudDecision(approved, false, reason);

        } catch (RestClientException ex) {
            log.warn("[fraud-check] transfer-service cannot reach fraud-service ({}), failing open: {}",
                    ex.getClass().getSimpleName(), ex.getMessage());
            return FraudDecision.ofSkipped();
        }
    }

    private String extractReason(Map<String, Object> response) {
        Object ruleResults = response.get("ruleResults");
        if (ruleResults instanceof List<?> list) {
            return list.stream()
                    .filter(r -> r instanceof Map<?, ?> m && Boolean.FALSE.equals(((Map<?,?>) m).get("passed")))
                    .map(r -> ((Map<?, ?>) r).get("reason"))
                    .filter(java.util.Objects::nonNull)
                    .map(Object::toString)
                    .findFirst()
                    .orElse("fraud rule triggered");
        }
        return "fraud rule triggered";
    }

    public record FraudDecision(boolean allowed, boolean skipped, String reason) {
        public static FraudDecision ofSkipped() {
            return new FraudDecision(true, true, "");
        }
    }
}
