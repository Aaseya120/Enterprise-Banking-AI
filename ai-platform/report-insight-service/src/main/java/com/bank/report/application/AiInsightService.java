package com.bank.report.application;

import com.bank.ai.gateway.model.AiRequest;
import com.bank.ai.gateway.model.AiResponse;
import com.bank.ai.gateway.router.ModelRouter;
import com.bank.report.domain.AccountMetrics;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * The method signature IS the guarantee: this class can only ever be
 * called with an AccountMetrics, never a raw transaction list, because
 * that's the only type its method accepts (plan section 26's "don't send
 * huge datasets to the LLM" rule, enforced by the type system rather than
 * by convention).
 */
@Service
public class AiInsightService {

    private static final String SYSTEM_PROMPT =
            "You are a banking insight assistant. You will be given aggregated account "
                    + "metrics for a period -- never raw transaction data. Write a concise, "
                    + "factual 2-3 sentence summary of the account's activity for a bank "
                    + "analyst. Only state what the numbers show; do not speculate about "
                    + "individual transactions you were not given.";

    private final ModelRouter modelRouter;

    public AiInsightService(ModelRouter modelRouter) {
        this.modelRouter = modelRouter;
    }

    public AiResponse summarize(AccountMetrics metrics) {
        String userMessage = String.format(
                "Account %s, last %d days: %d transactions, total debits %s, total credits %s, "
                        + "net change %s, largest single transaction %s.",
                metrics.accountId(), metrics.periodDays(), metrics.transactionCount(),
                metrics.totalDebits(), metrics.totalCredits(), metrics.netChange(),
                metrics.largestSingleTransaction());

        AiRequest request = new AiRequest(
                "report-" + metrics.accountId(), "report-insight-service", "REPORT_INSIGHT_V1", "1",
                SYSTEM_PROMPT, userMessage, List.of(), 0.2, 256,
                Map.of("taskType", "SIMPLE_FAQ"));

        return modelRouter.selectModel(request).generate(request);
    }
}
