package com.bank.report.domain;

import java.math.BigDecimal;

/**
 * The ONLY shape of data that ever reaches the LLM for this report type
 * (plan section 26: "Do not send huge database datasets directly to LLM
 * ... Database -> Aggregation -> Metrics -> AI -> Insight"). No individual
 * transaction ever appears in a prompt -- only these reduced numbers.
 */
public record AccountMetrics(
        String accountId,
        int periodDays,
        int transactionCount,
        BigDecimal totalDebits,
        BigDecimal totalCredits,
        BigDecimal netChange,
        BigDecimal largestSingleTransaction
) {
}
