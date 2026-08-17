package com.bank.fraud.domain;

import java.math.BigDecimal;

public record FraudEvaluationContext(
        String accountId,
        String counterpartyRef,
        BigDecimal amount,
        String currency,
        String channel
) {
}
