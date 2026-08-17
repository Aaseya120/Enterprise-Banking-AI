package com.bank.fraud.domain.rules;

import com.bank.fraud.domain.FraudEvaluationContext;
import com.bank.fraud.domain.FraudRule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ThresholdRule implements FraudRule {

    private final BigDecimal maxSingleTransactionAmount;

    public ThresholdRule(@Value("${bank.fraud.threshold.max-single-transaction:10000}") String maxAmount) {
        this.maxSingleTransactionAmount = new BigDecimal(maxAmount);
    }

    @Override
    public String name() {
        return "THRESHOLD";
    }

    @Override
    public RuleOutcome evaluate(FraudEvaluationContext context) {
        if (context.amount().compareTo(maxSingleTransactionAmount) > 0) {
            return RuleOutcome.fail("Amount " + context.amount() + " exceeds single-transaction limit of "
                    + maxSingleTransactionAmount);
        }
        return RuleOutcome.pass("Within single-transaction limit");
    }
}
