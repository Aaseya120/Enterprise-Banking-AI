package com.bank.loan.application;

import com.bank.common.events.AuditEventPublisher;
import com.bank.common.exception.BusinessException;
import com.bank.loan.domain.Loan;
import com.bank.loan.domain.LoanRepository;
import com.bank.loan.domain.LoanStatus;
import com.bank.loan.dto.ApplyLoanRequest;
import com.bank.loan.dto.LoanResponse;
import com.bank.loan.dto.RepayLoanRequest;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Loan lifecycle service. Key decisions:
 *
 * <p><b>Eligibility check</b>: apply() enforces a minimum credit-score
 * threshold (demo: PERSONAL/AUTO ≥ 650, HOME ≥ 700, EDUCATION ≥ 600;
 * score is passed in the request's purpose field as a demo shortcut —
 * a real system would call a credit bureau service). If the score is
 * absent or the threshold is unmet the loan is immediately REJECTED.
 *
 * <p><b>Audit</b>: LOAN_APPLIED, LOAN_APPROVED, LOAN_REJECTED,
 * LOAN_DISBURSED, LOAN_REPAYMENT_MADE, LOAN_CLOSED are emitted to
 * banking.audit.events via AuditEventPublisher.
 *
 * <p><b>Disburse + activate</b>: kept as separate steps so an operator
 * can confirm the funds have actually transferred before activating
 * repayment tracking. In the demo scaffold the account-service credit call
 * is not made (cross-service transaction would need the Saga pattern);
 * that is documented as a known gap.
 */
@Service
public class LoanApplicationService {

    // Demo eligibility thresholds by loan type.
    private static final Map<String, Integer> MIN_SCORE = Map.of(
            "PERSONAL", 650,
            "AUTO", 650,
            "HOME", 700,
            "EDUCATION", 600
    );

    private final LoanRepository loanRepository;
    private final AuditEventPublisher auditPublisher;

    public LoanApplicationService(LoanRepository loanRepository,
                                   AuditEventPublisher auditPublisher) {
        this.loanRepository = loanRepository;
        this.auditPublisher = auditPublisher;
    }

    // ── apply ─────────────────────────────────────────────────────────────

    @Transactional
    public LoanResponse apply(ApplyLoanRequest request) {
        // ── eligibility check ──────────────────────────────────────────────
        String type = request.loanType().toUpperCase();
        if (request.creditScore() != null) {
            int required = MIN_SCORE.getOrDefault(type, 0);
            if (request.creditScore() < required) {
                String reason = String.format(
                        "%s loan requires a minimum credit score of %d (provided: %d)",
                        type, required, request.creditScore());
                // Persist a REJECTED record so operators can audit the decline.
                Loan rejected = new Loan(
                        request.customerId(), request.accountId(), type,
                        request.principalAmount(), request.annualInterestRate(),
                        request.tenureMonths(), request.currency(), request.purpose());
                rejected.markUnderReview();
                rejected.reject(reason);
                rejected = loanRepository.save(rejected);
                auditPublisher.publish(
                        MDC.get("userId"), "LOAN_REJECTED",
                        "Loan/" + rejected.getLoanId(), false, "loan-service",
                        Map.of("reason", reason,
                                "creditScore", request.creditScore(),
                                "requiredScore", required));
                throw BusinessException.ruleViolation(reason);
            }
        }

        // ── create & save ─────────────────────────────────────────────────
        Loan loan = new Loan(
                request.customerId(), request.accountId(), type,
                request.principalAmount(), request.annualInterestRate(),
                request.tenureMonths(), request.currency(), request.purpose());
        loan.markUnderReview();
        loan = loanRepository.save(loan);

        auditPublisher.publish(
                MDC.get("userId"), "LOAN_APPLIED",
                "Loan/" + loan.getLoanId(),
                true, "loan-service",
                Map.of("customerId", request.customerId(),
                        "loanType", loan.getLoanType(),
                        "amount", request.principalAmount()));
        return LoanResponse.from(loan);
    }

    // ── approve / reject ──────────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyRole('BANK_STAFF','ADMIN')")
    public LoanResponse approve(String loanId) {
        Loan loan = findOrThrow(loanId);
        loan.approve();
        loan = loanRepository.save(loan);
        auditPublisher.publish(
                MDC.get("userId"), "LOAN_APPROVED",
                "Loan/" + loanId, true, "loan-service",
                Map.of("emi", loan.getEmi(), "tenureMonths", loan.getTenureMonths()));
        return LoanResponse.from(loan);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('BANK_STAFF','ADMIN')")
    public LoanResponse reject(String loanId, String reason) {
        Loan loan = findOrThrow(loanId);
        try {
            loan.reject(reason);
        } catch (IllegalStateException e) {
            throw BusinessException.ruleViolation(e.getMessage());
        }
        loan = loanRepository.save(loan);
        auditPublisher.publish(
                MDC.get("userId"), "LOAN_REJECTED",
                "Loan/" + loanId, false, "loan-service",
                Map.of("reason", reason));
        return LoanResponse.from(loan);
    }

    // ── disburse + activate ───────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyRole('BANK_STAFF','ADMIN')")
    public LoanResponse disburse(String loanId) {
        Loan loan = findOrThrow(loanId);
        try {
            loan.disburse();
        } catch (IllegalStateException e) {
            throw BusinessException.ruleViolation(e.getMessage());
        }
        // NOTE: in a full deployment a credit to loan.getAccountId() for
        // loan.getPrincipalAmount() would be made here via account-service,
        // coordinated through a Saga. Documented gap — not implemented.
        loan.activate();
        loan = loanRepository.save(loan);
        auditPublisher.publish(
                MDC.get("userId"), "LOAN_DISBURSED",
                "Loan/" + loanId, true, "loan-service",
                Map.of("accountId", loan.getAccountId(),
                        "amount", loan.getPrincipalAmount()));
        return LoanResponse.from(loan);
    }

    // ── repay ─────────────────────────────────────────────────────────────

    @Transactional
    public LoanResponse repay(String loanId, RepayLoanRequest request) {
        Loan loan = findOrThrow(loanId);
        boolean closed;
        try {
            closed = loan.repay(request.amount());
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw BusinessException.ruleViolation(e.getMessage());
        }
        loan = loanRepository.save(loan);

        if (closed) {
            auditPublisher.publish(
                    MDC.get("userId"), "LOAN_CLOSED",
                    "Loan/" + loanId, true, "loan-service",
                    Map.of("finalRepayment", request.amount()));
        } else {
            auditPublisher.publish(
                    MDC.get("userId"), "LOAN_REPAYMENT_MADE",
                    "Loan/" + loanId, true, "loan-service",
                    Map.of("amount", request.amount(),
                            "outstanding", loan.getOutstandingBalance()));
        }
        return LoanResponse.from(loan);
    }

    // ── queries ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public LoanResponse getLoan(String loanId) {
        return LoanResponse.from(findOrThrow(loanId));
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getLoansForCustomer(String customerId) {
        return loanRepository.findByCustomerId(customerId).stream()
                .map(LoanResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<LoanResponse> getByStatus(LoanStatus status, Pageable pageable) {
        return loanRepository.findByStatus(status, pageable).map(LoanResponse::from);
    }

    // ── helper ────────────────────────────────────────────────────────────

    private Loan findOrThrow(String loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> BusinessException.notFound("Loan not found: " + loanId));
    }
}
