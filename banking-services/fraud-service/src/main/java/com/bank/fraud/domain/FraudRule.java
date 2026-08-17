package com.bank.fraud.domain;

/**
 * Every rule is independent, deterministic, and cheap to explain -- the
 * opposite design point from fraud-ai-service's model-based scoring. The
 * engine runs ALL rules and combines them (see FraudRuleEngine); a single
 * rule failing is enough to deny, but every rule's outcome is returned so
 * the caller can see exactly why.
 */
public interface FraudRule {

    String name();

    RuleOutcome evaluate(FraudEvaluationContext context);

    record RuleOutcome(boolean passed, String reason) {
        public static RuleOutcome pass(String reason) {
            return new RuleOutcome(true, reason);
        }

        public static RuleOutcome fail(String reason) {
            return new RuleOutcome(false, reason);
        }
    }
}
