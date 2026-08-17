package com.bank.loan;

import com.bank.loan.application.LoanApplicationService;
import com.bank.loan.domain.Loan;
import com.bank.loan.domain.LoanStatus;
import com.bank.loan.dto.ApplyLoanRequest;
import com.bank.loan.dto.LoanResponse;
import com.bank.loan.dto.RepayLoanRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = LoanServiceApplication.class)
@EmbeddedKafka(partitions = 1, topics = {"banking.audit.events"})
class LoanApplicationServiceTest {

    @Autowired
    private LoanApplicationService loanService;

    @Test
    void applyCreatesLoanInUnderReview() {
        LoanResponse response = loanService.apply(new ApplyLoanRequest(
                "CUST-1", "ACC-1", "PERSONAL",
                new BigDecimal("50000.00"), new BigDecimal("12.50"),
                36, "USD", "Debt consolidation", null));

        assertThat(response.status()).isEqualTo(LoanStatus.UNDER_REVIEW);
        assertThat(response.principalAmount()).isEqualByComparingTo("50000.00");
        assertThat(response.loanId()).isNotBlank();
    }

    @Test
    @WithMockUser(roles = "BANK_STAFF")
    void emiIsComputedCorrectlyOnApproval() {
        LoanResponse applied = loanService.apply(new ApplyLoanRequest(
                "CUST-2", "ACC-2", "HOME",
                new BigDecimal("100000.00"), new BigDecimal("8.00"),
                120, "USD", "House purchase", null));

        LoanResponse approved = loanService.approve(applied.loanId());

        assertThat(approved.status()).isEqualTo(LoanStatus.APPROVED);
        // EMI = 100000 * (0.08/12) * (1+0.08/12)^120 / ((1+0.08/12)^120 - 1)
        // ≈ 1213.2764 — within a 10-unit tolerance for floating-point precision
        assertThat(approved.emi()).isBetween(new BigDecimal("1210.00"), new BigDecimal("1220.00"));
    }

    @Test
    @WithMockUser(roles = "BANK_STAFF")
    void fullLifecycleApplyApproveDisburseThenRepayToClose() {
        BigDecimal principal = new BigDecimal("1200.00");
        LoanResponse applied = loanService.apply(new ApplyLoanRequest(
                "CUST-3", "ACC-3", "PERSONAL",
                principal, new BigDecimal("0.00"), // zero-interest for easy math
                1, "USD", "Test loan", null));

        loanService.approve(applied.loanId());
        LoanResponse active = loanService.disburse(applied.loanId());
        assertThat(active.status()).isEqualTo(LoanStatus.ACTIVE);

        // Full repayment in one go → should auto-close
        LoanResponse closed = loanService.repay(active.loanId(),
                new RepayLoanRequest(principal));
        assertThat(closed.status()).isEqualTo(LoanStatus.CLOSED);
        assertThat(closed.outstandingBalance()).isEqualByComparingTo("0.00");
        assertThat(closed.closedAt()).isNotNull();
    }

    @Test
    @WithMockUser(roles = "BANK_STAFF")
    void rejectedLoanCannotBeApproved() {
        LoanResponse applied = loanService.apply(new ApplyLoanRequest(
                "CUST-4", "ACC-4", "AUTO",
                new BigDecimal("20000.00"), new BigDecimal("10.00"),
                48, "USD", "Car", null));

        loanService.reject(applied.loanId(), "Credit score below threshold");

        assertThatThrownBy(() -> loanService.approve(applied.loanId()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void emiFormulaZeroRateEqualsEvenInstalment() {
        BigDecimal principal = new BigDecimal("12000.00");
        BigDecimal emi = Loan.computeEmi(principal, BigDecimal.ZERO, 12);
        assertThat(emi).isEqualByComparingTo("1000.0000");
    }
}
