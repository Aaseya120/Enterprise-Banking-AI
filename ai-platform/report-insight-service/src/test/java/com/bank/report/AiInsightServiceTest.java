package com.bank.report;

import com.bank.ai.gateway.client.AiModelClient;
import com.bank.ai.gateway.model.AiRequest;
import com.bank.ai.gateway.model.AiResponse;
import com.bank.ai.gateway.model.ModelProvider;
import com.bank.ai.gateway.router.ModelRouter;
import com.bank.report.application.AiInsightService;
import com.bank.report.domain.AccountMetrics;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiInsightServiceTest {

    @Test
    void promptSentToModelContainsOnlyAggregatedNumbersNeverRawTransactionDetails() {
        ModelRouter router = mock(ModelRouter.class);
        AiModelClient client = mock(AiModelClient.class);
        when(router.selectModel(any())).thenReturn(client);
        when(client.generate(any())).thenReturn(new AiResponse(
                "Account had moderate activity.", ModelProvider.OPENAI, "gpt-4o (demo)", List.of(), 0, 0, 5L, false));

        AiInsightService service = new AiInsightService(router);
        AccountMetrics metrics = new AccountMetrics(
                "ACC-1", 30, 3, new BigDecimal("150.00"), new BigDecimal("300.00"),
                new BigDecimal("150.00"), new BigDecimal("300.00"));

        AiResponse response = service.summarize(metrics);

        assertThat(response.content()).isEqualTo("Account had moderate activity.");

        ArgumentCaptor<AiRequest> captor = ArgumentCaptor.forClass(AiRequest.class);
        verify(client).generate(captor.capture());
        String userMessage = captor.getValue().userMessage();

        // The prompt should contain the aggregate numbers...
        assertThat(userMessage).contains("150.00").contains("300.00").contains("3 transactions");
        // ...and nothing that looks like a per-transaction identifier or raw record.
        assertThat(userMessage).doesNotContain("transactionId").doesNotContain("TXN-");
    }
}
