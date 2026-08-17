package com.bank.payment.client;

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
 * Calls fraud-service's POST /api/v1/fraud/evaluate synchronously before a
 * payment completes. The call is fail-open: if fraud-service is unreachable
 * (network error, timeout, non-2xx) the returned FraudDecision reports
 * skipped=true and allowed=true so the payment proceeds.
 *
 * <p>This is the business-rule engine check (velocity, blacklist, threshold).
 * The async AI fraud scoring via fraud-ai-service is a separate, deeper check
 * applied to transfers after they are already in PENDING_FRAUD_REVIEW state.
 */
@Component
public class FraudServiceClient {

    private static final Logger log = LoggerFactory.getLogger(FraudServiceClient.class);

    private final RestClient restClient;

    public FraudServiceClient(
            @Value("${bank.services.fraud-service-url:http://fraud-service:8093}") String fraudServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(fraudServiceUrl)
                .build();
    }

    /**
     * @return FraudDecision.allowed()=false when the payment should be blocked;
     *         FraudDecision.skipped()=true when the check could not be performed.
     */
    public FraudDecision evaluate(String accountId, BigDecimal amount, String currency) {
        try {
            Map<String, Object> request = Map.of(
                    "accountId", accountId,
                    "amount", amount,
                    "currency", currency,
                    "channel", "API"
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
            log.warn("[fraud-check] payment-service cannot reach fraud-service ({}), failing open: {}",
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

    /**
     * Result of the synchronous fraud pre-check.
     *
     * @param allowed true → payment may proceed; false → must be blocked
     * @param skipped true → fraud-service was unreachable; caller decides fail-open/closed
     * @param reason  human-readable rejection reason (empty when allowed or skipped)
     */
    public record FraudDecision(boolean allowed, boolean skipped, String reason) {
        public static FraudDecision ofSkipped() {
            return new FraudDecision(true, true, "");
        }
    }
}
