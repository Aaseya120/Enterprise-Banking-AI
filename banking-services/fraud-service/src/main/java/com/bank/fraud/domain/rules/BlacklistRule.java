package com.bank.fraud.domain.rules;

import com.bank.fraud.domain.BlacklistEntryRepository;
import com.bank.fraud.domain.FraudEvaluationContext;
import com.bank.fraud.domain.FraudRule;
import org.springframework.stereotype.Component;

@Component
public class BlacklistRule implements FraudRule {

    private final BlacklistEntryRepository blacklistEntryRepository;

    public BlacklistRule(BlacklistEntryRepository blacklistEntryRepository) {
        this.blacklistEntryRepository = blacklistEntryRepository;
    }

    @Override
    public String name() {
        return "BLACKLIST";
    }

    @Override
    public RuleOutcome evaluate(FraudEvaluationContext context) {
        if (blacklistEntryRepository.existsByEntityRef(context.accountId())) {
            return RuleOutcome.fail("Account " + context.accountId() + " is blacklisted");
        }
        if (context.counterpartyRef() != null && blacklistEntryRepository.existsByEntityRef(context.counterpartyRef())) {
            return RuleOutcome.fail("Counterparty " + context.counterpartyRef() + " is blacklisted");
        }
        return RuleOutcome.pass("Neither party is blacklisted");
    }
}
