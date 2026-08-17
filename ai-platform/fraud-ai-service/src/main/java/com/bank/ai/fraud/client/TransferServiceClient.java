package com.bank.ai.fraud.client;

import com.bank.common.crypto.CryptoUtil;
import com.bank.common.exception.AiPlatformException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * POST /transfers/{id}/fraud-decision would otherwise be callable by
 * anyone who can reach transfer-service's port -- there's nothing else
 * distinguishing "the real fraud-ai-service" from "an attacker directly
 * approving their own high-risk transfer." HMAC-SHA256 signing (over a
 * canonical string derived from the request, using a secret only
 * fraud-ai-service and transfer-service know) closes that gap: transfer-
 * service's TransferController verifies the signature before accepting
 * the decision (see its javadoc for the shared-secret caveat).
 */
@Component
public class TransferServiceClient {

    private final WebClient webClient;
    private final String hmacSecret;

    public TransferServiceClient(
            @Value("${bank.services.transfer-service.base-url:http://localhost:8084}") String baseUrl,
            @Value("${bank.crypto.fraud-decision-hmac-secret}") String hmacSecretPassphrase) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.hmacSecret = java.util.Base64.getEncoder().encodeToString(
                hmacSecretPassphrase.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public void submitFraudDecision(String transferId, boolean approved) {
        try {
            String canonicalPayload = canonicalize(transferId, approved);
            String signature = CryptoUtil.hmacSha256(canonicalPayload, hmacSecret);

            webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/transfers/{id}/fraud-decision")
                            .queryParam("approved", approved)
                            .build(transferId))
                    .header("X-Signature", signature)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            throw AiPlatformException.toolInvocationError(
                    "Failed to submit fraud decision for transfer " + transferId, e);
        }
    }

    /**
     * Must match exactly what TransferController reconstructs to verify --
     * see that class's javadoc. Keeping this as one small shared-shape
     * method (rather than inlining the string format at each call site)
     * is what keeps the two services from silently drifting apart.
     */
    static String canonicalize(String transferId, boolean approved) {
        return "transferId=" + transferId + ";approved=" + approved;
    }
}
