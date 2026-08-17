package com.bank.loan.controller;

import com.bank.loan.application.LoanApplicationService;
import com.bank.loan.domain.LoanStatus;
import com.bank.loan.dto.ApplyLoanRequest;
import com.bank.loan.dto.LoanResponse;
import com.bank.loan.dto.RepayLoanRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the loan lifecycle.
 *
 * POST   /api/v1/loans                          — apply
 * GET    /api/v1/loans/{loanId}                 — get by ID
 * GET    /api/v1/loans/customer/{customerId}     — list for customer
 * GET    /api/v1/loans?status=ACTIVE             — list by status (pageable)
 * POST   /api/v1/loans/{loanId}/approve          — approve (BANK_STAFF/ADMIN)
 * POST   /api/v1/loans/{loanId}/reject           — reject  (BANK_STAFF/ADMIN)
 * POST   /api/v1/loans/{loanId}/disburse         — disburse (BANK_STAFF/ADMIN)
 * POST   /api/v1/loans/{loanId}/repay            — repayment
 */
@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {

    private final LoanApplicationService loanService;

    public LoanController(LoanApplicationService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    public ResponseEntity<LoanResponse> apply(@Valid @RequestBody ApplyLoanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.apply(request));
    }

    @GetMapping("/{loanId}")
    public LoanResponse getLoan(@PathVariable String loanId) {
        return loanService.getLoan(loanId);
    }

    @GetMapping("/customer/{customerId}")
    public List<LoanResponse> getLoansForCustomer(@PathVariable String customerId) {
        return loanService.getLoansForCustomer(customerId);
    }

    @GetMapping
    public Page<LoanResponse> getByStatus(
            @RequestParam(defaultValue = "ACTIVE") LoanStatus status,
            Pageable pageable) {
        return loanService.getByStatus(status, pageable);
    }

    @PostMapping("/{loanId}/approve")
    public LoanResponse approve(@PathVariable String loanId) {
        return loanService.approve(loanId);
    }

    @PostMapping("/{loanId}/reject")
    public LoanResponse reject(@PathVariable String loanId,
                                @RequestParam String reason) {
        return loanService.reject(loanId, reason);
    }

    @PostMapping("/{loanId}/disburse")
    public LoanResponse disburse(@PathVariable String loanId) {
        return loanService.disburse(loanId);
    }

    @PostMapping("/{loanId}/repay")
    public LoanResponse repay(@PathVariable String loanId,
                               @Valid @RequestBody RepayLoanRequest request) {
        return loanService.repay(loanId, request);
    }
}
