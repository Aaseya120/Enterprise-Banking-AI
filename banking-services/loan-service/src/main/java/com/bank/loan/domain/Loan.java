package com.bank.loan.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Aggregate root for a loan. Owns the complete lifecycle:
 * APPLIED → UNDER_REVIEW → APPROVED / REJECTED → DISBURSED → ACTIVE → CLOSED
 *
 * <p>Amortisation: the monthly EMI is computed from the principal, annual
 * interest rate, and tenure using the standard formula
 *   EMI = P * r * (1+r)^n / ((1+r)^n - 1)
 * where r = monthlyRate and n = tenureMonths. This is stored on the entity
 * so the borrower always sees the agreed EMI regardless of rate changes.
 *
 * <p>Each repayment decrements outstandingBalance. When it reaches zero the
 * loan is automatically CLOSED.
 */
@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String loanId;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String accountId;   // disbursement target

    @Column(nullable = false)
    private String loanType;    // PERSONAL, HOME, AUTO, EDUCATION

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal principalAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal outstandingBalance;

    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal annualInterestRate;

    @Column(nullable = false)
    private int tenureMonths;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal emi;     // monthly EMI, computed at approval

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    @Column(nullable = false)
    private String purpose;

    private String rejectionReason;

    @Column(nullable = false)
    private Instant appliedAt;

    private Instant approvedAt;
    private Instant disbursedAt;
    private Instant closedAt;

    private LocalDate nextPaymentDue;

    protected Loan() {} // JPA

    public Loan(String customerId, String accountId, String loanType,
                BigDecimal principalAmount, BigDecimal annualInterestRate,
                int tenureMonths, String currency, String purpose) {
        this.customerId = customerId;
        this.accountId = accountId;
        this.loanType = loanType;
        this.principalAmount = principalAmount;
        this.outstandingBalance = principalAmount;
        this.annualInterestRate = annualInterestRate;
        this.tenureMonths = tenureMonths;
        this.currency = currency;
        this.purpose = purpose;
        this.status = LoanStatus.APPLIED;
        this.appliedAt = Instant.now();
        this.emi = BigDecimal.ZERO; // computed at approval
    }

    // ── lifecycle transitions ──────────────────────────────────────────────

    public void markUnderReview() {
        require(LoanStatus.APPLIED, "Only APPLIED loans can enter UNDER_REVIEW");
        this.status = LoanStatus.UNDER_REVIEW;
    }

    public void approve() {
        require(LoanStatus.UNDER_REVIEW, "Only UNDER_REVIEW loans can be APPROVED");
        this.status = LoanStatus.APPROVED;
        this.approvedAt = Instant.now();
        this.emi = computeEmi(principalAmount, annualInterestRate, tenureMonths);
    }

    public void reject(String reason) {
        if (status != LoanStatus.APPLIED && status != LoanStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Cannot reject a loan in status " + status);
        }
        this.status = LoanStatus.REJECTED;
        this.rejectionReason = reason;
    }

    public void disburse() {
        require(LoanStatus.APPROVED, "Only APPROVED loans can be DISBURSED");
        this.status = LoanStatus.DISBURSED;
        this.disbursedAt = Instant.now();
    }

    public void activate() {
        require(LoanStatus.DISBURSED, "Only DISBURSED loans can be ACTIVE");
        this.status = LoanStatus.ACTIVE;
        this.nextPaymentDue = LocalDate.now().plusMonths(1);
    }

    /**
     * Records a repayment. Reduces outstanding balance; if it reaches zero
     * (or below due to rounding), the loan is CLOSED automatically.
     *
     * @param amount the repayment amount
     * @return true if the loan was auto-closed by this repayment
     */
    public boolean repay(BigDecimal amount) {
        if (status != LoanStatus.ACTIVE) {
            throw new IllegalStateException("Cannot repay a loan in status " + status);
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Repayment amount must be positive");
        }
        this.outstandingBalance = this.outstandingBalance.subtract(amount)
                .max(BigDecimal.ZERO);
        if (this.outstandingBalance.compareTo(BigDecimal.ZERO) == 0) {
            this.status = LoanStatus.CLOSED;
            this.closedAt = Instant.now();
            this.nextPaymentDue = null;
            return true;
        }
        this.nextPaymentDue = nextPaymentDue != null
                ? nextPaymentDue.plusMonths(1) : LocalDate.now().plusMonths(1);
        return false;
    }

    // ── EMI formula ────────────────────────────────────────────────────────

    /**
     * EMI = P × r × (1+r)^n / ((1+r)^n − 1)
     * where r = annualRate / 12 / 100, n = tenureMonths.
     */
    public static BigDecimal computeEmi(BigDecimal principal,
                                         BigDecimal annualRatePct,
                                         int tenureMonths) {
        if (annualRatePct.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), 4, RoundingMode.HALF_UP);
        }
        double r = annualRatePct.doubleValue() / 12.0 / 100.0;
        double n = tenureMonths;
        double pow = Math.pow(1 + r, n);
        double emi = (principal.doubleValue() * r * pow) / (pow - 1);
        return BigDecimal.valueOf(emi).setScale(4, RoundingMode.HALF_UP);
    }

    // ── guard ──────────────────────────────────────────────────────────────

    private void require(LoanStatus expected, String msg) {
        if (status != expected) throw new IllegalStateException(msg + " (current: " + status + ")");
    }

    // ── accessors ──────────────────────────────────────────────────────────

    public String getLoanId() { return loanId; }
    public String getCustomerId() { return customerId; }
    public String getAccountId() { return accountId; }
    public String getLoanType() { return loanType; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public BigDecimal getAnnualInterestRate() { return annualInterestRate; }
    public int getTenureMonths() { return tenureMonths; }
    public BigDecimal getEmi() { return emi; }
    public String getCurrency() { return currency; }
    public LoanStatus getStatus() { return status; }
    public String getPurpose() { return purpose; }
    public String getRejectionReason() { return rejectionReason; }
    public Instant getAppliedAt() { return appliedAt; }
    public Instant getApprovedAt() { return approvedAt; }
    public Instant getDisbursedAt() { return disbursedAt; }
    public Instant getClosedAt() { return closedAt; }
    public LocalDate getNextPaymentDue() { return nextPaymentDue; }
}
