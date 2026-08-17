package com.bank.report.application;

import com.bank.report.client.TransactionServiceClient;
import com.bank.report.domain.AccountMetrics;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Implements "Database -> Aggregation -> Metrics" (plan section 26). This
 * is the ONLY class in the service allowed to hold a raw transaction list
 * in memory; everything it returns has already been reduced to a handful
 * of numbers. AiInsightService depends on AccountMetrics, never on this
 * class's intermediate List<Map<String,Object>>.
 */
@Service
public class MetricsAggregationService {

    private final TransactionServiceClient transactionServiceClient;

    public MetricsAggregationService(TransactionServiceClient transactionServiceClient) {
        this.transactionServiceClient = transactionServiceClient;
    }

    public AccountMetrics aggregate(String accountId, int periodDays) {
        List<Map<String, Object>> transactions = transactionServiceClient.getRecentTransactions(accountId, periodDays);

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal largest = BigDecimal.ZERO;

        for (Map<String, Object> txn : transactions) {
            BigDecimal amount = toBigDecimal(txn.get("amount"));
            String type = String.valueOf(txn.get("transactionType"));

            if ("DEBIT".equals(type) || "FEE".equals(type)) {
                totalDebits = totalDebits.add(amount);
            } else if ("CREDIT".equals(type) || "INTEREST".equals(type)) {
                totalCredits = totalCredits.add(amount);
            }
            if (amount.compareTo(largest) > 0) {
                largest = amount;
            }
        }

        return new AccountMetrics(
                accountId, periodDays, transactions.size(), totalDebits, totalCredits,
                totalCredits.subtract(totalDebits), largest);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
