package com.bank.report;

import com.bank.report.application.MetricsAggregationService;
import com.bank.report.client.TransactionServiceClient;
import com.bank.report.domain.AccountMetrics;
import org.junit.jupiter.api.Test;


import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetricsAggregationServiceTest {

    @Test
    void aggregatesDebitsCreditsAndNetChangeCorrectly() {
        TransactionServiceClient client = mock(TransactionServiceClient.class);
        when(client.getRecentTransactions(eq("ACC-1"), any(Integer.class))).thenReturn(List.of(
                Map.of("amount", "100.00", "transactionType", "DEBIT", "createdAt", Instant.now().toString()),
                Map.of("amount", "50.00", "transactionType", "DEBIT", "createdAt", Instant.now().toString()),
                Map.of("amount", "300.00", "transactionType", "CREDIT", "createdAt", Instant.now().toString())
        ));

        MetricsAggregationService service = new MetricsAggregationService(client);
        AccountMetrics metrics = service.aggregate("ACC-1", 30);

        assertThat(metrics.transactionCount()).isEqualTo(3);
        assertThat(metrics.totalDebits()).isEqualByComparingTo("150.00");
        assertThat(metrics.totalCredits()).isEqualByComparingTo("300.00");
        assertThat(metrics.netChange()).isEqualByComparingTo("150.00");
        assertThat(metrics.largestSingleTransaction()).isEqualByComparingTo("300.00");
    }

    @Test
    void emptyTransactionListProducesZeroedMetricsNotAnError() {
        TransactionServiceClient client = mock(TransactionServiceClient.class);
        when(client.getRecentTransactions(eq("ACC-2"), any(Integer.class))).thenReturn(List.of());

        MetricsAggregationService service = new MetricsAggregationService(client);
        AccountMetrics metrics = service.aggregate("ACC-2", 7);

        assertThat(metrics.transactionCount()).isZero();
        assertThat(metrics.netChange()).isEqualByComparingTo("0");
    }
}
