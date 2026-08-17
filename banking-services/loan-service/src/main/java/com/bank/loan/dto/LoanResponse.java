package com.bank.loan.dto;

import com.bank.loan.domain.Loan;
import com.bank.loan.domain.LoanStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record LoanResponse(
        String loanId,
        String customerId,
        String accountId,
        String loanType,
        BigDecimal principalAmount,
        BigDecimal outstandingBalance,
        BigDecimal annualInterestRate,
        int tenureMonths,
        BigDecimal emi,
        String currency,
        LoanStatus status,
        String purpose,
        String rejectionReason,
        Instant appliedAt,
        Instant approvedAt,
        Instant disbursedAt,
        Instant closedAt,
        LocalDate nextPaymentDue
) {
    public static LoanResponse from(Loan loan) {
        return new LoanResponse(
                loan.getLoanId(),
                loan.getCustomerId(),
                loan.getAccountId(),
                loan.getLoanType(),
                loan.getPrincipalAmount(),
                loan.getOutstandingBalance(),
                loan.getAnnualInterestRate(),
                loan.getTenureMonths(),
                loan.getEmi(),
                loan.getCurrency(),
                loan.getStatus(),
                loan.getPurpose(),
                loan.getRejectionReason(),
                loan.getAppliedAt(),
                loan.getApprovedAt(),
                loan.getDisbursedAt(),
                loan.getClosedAt(),
                loan.getNextPaymentDue()
        );
    }
}
