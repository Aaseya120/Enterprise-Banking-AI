package com.bank.ai.fraud.service;

import com.bank.ai.fraud.model.RiskScore;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Feature extraction + risk model (section 25). This is a small,
 * explainable rule set rather than a trained model, so the fraud saga can
 * be demonstrated end-to-end without external dependencies. Swap the body
 * of score() for a real model/service call (e.g. via ai-model-gateway with
 * taskType=FRAUD_ANALYSIS) -- the Kafka consumer and callback plumbing
 * around it does not need to change.
 *
 * IMPORTANT (section 25/44): this AI recommendation is a decision INPUT,
 * not a bypass of business rules -- transfer-service still owns whether the
 * transfer actually proceeds, via the /fraud-decision callback.
 */
@Service
public class RiskScoringService {

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("10000");
    private static final BigDecimal MEDIUM_VALUE_THRESHOLD = new BigDecimal("5000");

    public RiskScore score(String transferId, Map<String, Object> eventPayload) {
        BigDecimal amount = toBigDecimal(eventPayload.get("amount"));

        if (amount.compareTo(HIGH_VALUE_THRESHOLD) >= 0) {
            return new RiskScore(transferId, 0.85, RiskScore.RiskLevel.HIGH,
                    "Transfer amount " + amount + " exceeds high-value threshold");
        }
        if (amount.compareTo(MEDIUM_VALUE_THRESHOLD) >= 0) {
            return new RiskScore(transferId, 0.45, RiskScore.RiskLevel.MEDIUM,
                    "Transfer amount " + amount + " exceeds medium-value threshold");
        }
        return new RiskScore(transferId, 0.05, RiskScore.RiskLevel.LOW, "Amount within normal range");
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return new BigDecimal(String.valueOf(value));
    }
}
