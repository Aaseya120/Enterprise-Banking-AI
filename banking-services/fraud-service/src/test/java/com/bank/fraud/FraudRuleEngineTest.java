package com.bank.fraud;

import com.bank.fraud.application.BlacklistAdminService;
import com.bank.fraud.application.FraudRuleEngine;
import com.bank.fraud.dto.AddBlacklistEntryRequest;
import com.bank.fraud.dto.FraudEvaluationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = FraudServiceApplication.class)
class FraudRuleEngineTest {

    @Autowired
    private FraudRuleEngine fraudRuleEngine;

    @Autowired
    private BlacklistAdminService blacklistAdminService;

    @Test
    void approvesANormalLowValueTransaction() {
        var response = fraudRuleEngine.evaluate(
                new FraudEvaluationRequest("ACC-100", "ACC-200", new BigDecimal("50.00"), "USD", "WEB"));

        assertThat(response.decision()).isEqualTo("APPROVED");
        assertThat(response.ruleResults()).allMatch(FraudEvaluationRequest.RuleResult::passed);
    }

    @Test
    void deniesAnAmountOverTheThreshold() {
        var response = fraudRuleEngine.evaluate(
                new FraudEvaluationRequest("ACC-101", "ACC-201", new BigDecimal("15000.00"), "USD", "WEB"));

        assertThat(response.decision()).isEqualTo("DENIED");
        assertThat(response.ruleResults()).anyMatch(r -> r.ruleName().equals("THRESHOLD") && !r.passed());
    }

    @Test
    @WithMockUser(roles = "COMPLIANCE_OFFICER")
    void deniesATransactionInvolvingABlacklistedAccount() {
        blacklistAdminService.add(new AddBlacklistEntryRequest("ACC-BAD", "Known fraud ring"));

        var response = fraudRuleEngine.evaluate(
                new FraudEvaluationRequest("ACC-102", "ACC-BAD", new BigDecimal("10.00"), "USD", "WEB"));

        assertThat(response.decision()).isEqualTo("DENIED");
        assertThat(response.ruleResults()).anyMatch(r -> r.ruleName().equals("BLACKLIST") && !r.passed());
    }

    @Test
    void deniesAfterExceedingVelocityLimit() {
        String accountId = "ACC-VELOCITY";
        for (int i = 0; i < 5; i++) {
            fraudRuleEngine.evaluate(new FraudEvaluationRequest(accountId, null, new BigDecimal("10.00"), "USD", "WEB"));
        }
        var sixth = fraudRuleEngine.evaluate(
                new FraudEvaluationRequest(accountId, null, new BigDecimal("10.00"), "USD", "WEB"));

        assertThat(sixth.decision()).isEqualTo("DENIED");
        assertThat(sixth.ruleResults()).anyMatch(r -> r.ruleName().equals("VELOCITY") && !r.passed());
    }
}
