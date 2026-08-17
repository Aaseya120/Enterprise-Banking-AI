package com.bank.fraud.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record FraudEvaluationRequest(
        @NotBlank String accountId,
        String counterpartyRef,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String currency,
        String channel
) {
    public record RuleResult(String ruleName, boolean passed, String reason) {
    }

    public record Response(String decision, List<RuleResult> ruleResults) {
    }
}
