package com.bank.fraud.domain.rules;

import com.bank.fraud.domain.EvaluationLogRepository;
import com.bank.fraud.domain.FraudEvaluationContext;
import com.bank.fraud.domain.FraudRule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class VelocityRule implements FraudRule {

    private final EvaluationLogRepository evaluationLogRepository;
    private final int maxTransactionsPerWindow;
    private final long windowSeconds;

    public VelocityRule(EvaluationLogRepository evaluationLogRepository,
                         @Value("${bank.fraud.velocity.max-transactions:5}") int maxTransactionsPerWindow,
                         @Value("${bank.fraud.velocity.window-seconds:60}") long windowSeconds) {
        this.evaluationLogRepository = evaluationLogRepository;
        this.maxTransactionsPerWindow = maxTransactionsPerWindow;
        this.windowSeconds = windowSeconds;
    }

    @Override
    public String name() {
        return "VELOCITY";
    }

    @Override
    public RuleOutcome evaluate(FraudEvaluationContext context) {
        Instant since = Instant.now().minusSeconds(windowSeconds);
        long recentCount = evaluationLogRepository.countByAccountIdAndEvaluatedAtAfter(context.accountId(), since);
        if (recentCount >= maxTransactionsPerWindow) {
            return RuleOutcome.fail("Account " + context.accountId() + " had " + recentCount
                    + " evaluations in the last " + windowSeconds + "s, exceeding limit of "
                    + maxTransactionsPerWindow);
        }
        return RuleOutcome.pass("Transaction velocity within limits (" + recentCount + "/" + maxTransactionsPerWindow + ")");
    }
}
