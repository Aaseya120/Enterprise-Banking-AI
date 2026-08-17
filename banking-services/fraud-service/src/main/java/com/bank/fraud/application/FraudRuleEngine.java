package com.bank.fraud.application;

import com.bank.fraud.domain.EvaluationLog;
import com.bank.fraud.domain.EvaluationLogRepository;
import com.bank.fraud.domain.FraudEvaluationContext;
import com.bank.fraud.domain.FraudRule;
import com.bank.fraud.dto.FraudEvaluationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Runs every registered FraudRule and combines the results: ANY rule
 * failing denies the transaction (deny-by-default on a rule hit, not
 * majority vote), and every rule's outcome is returned so the caller has
 * a full explanation, not just a boolean. This is the deterministic floor
 * referenced in the plan's "AI recommendations must not bypass approved
 * fraud/business rules" principle (section 25/44) -- a real deployment
 * would have payment-service and transfer-service call this synchronously
 * before committing, in addition to (not instead of) fraud-ai-service's
 * asynchronous risk scoring.
 */
@Service
public class FraudRuleEngine {

    private final List<FraudRule> rules;
    private final EvaluationLogRepository evaluationLogRepository;

    public FraudRuleEngine(List<FraudRule> rules, EvaluationLogRepository evaluationLogRepository) {
        this.rules = rules;
        this.evaluationLogRepository = evaluationLogRepository;
    }

    @Transactional
    public FraudEvaluationRequest.Response evaluate(FraudEvaluationRequest request) {
        FraudEvaluationContext context = new FraudEvaluationContext(
                request.accountId(), request.counterpartyRef(), request.amount(),
                request.currency(), request.channel());

        List<FraudEvaluationRequest.RuleResult> results = rules.stream()
                .map(rule -> {
                    FraudRule.RuleOutcome outcome = rule.evaluate(context);
                    return new FraudEvaluationRequest.RuleResult(rule.name(), outcome.passed(), outcome.reason());
                })
                .toList();

        boolean anyFailed = results.stream().anyMatch(r -> !r.passed());
        String decision = anyFailed ? "DENIED" : "APPROVED";

        evaluationLogRepository.save(new EvaluationLog(request.accountId(), request.amount(), decision));

        return new FraudEvaluationRequest.Response(decision, results);
    }
}
