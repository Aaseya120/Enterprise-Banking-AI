package com.bank.transaction.controller;

import com.bank.transaction.application.TransactionApplicationService;
import com.bank.transaction.dto.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionApplicationService transactionApplicationService;

    public TransactionController(TransactionApplicationService transactionApplicationService) {
        this.transactionApplicationService = transactionApplicationService;
    }

    @GetMapping("/account/{accountId}")
    public Page<TransactionResponse> getStatement(@PathVariable String accountId, Pageable pageable) {
        return transactionApplicationService.getStatement(accountId, pageable);
    }

    @GetMapping("/{transactionId}")
    public TransactionResponse getByTransactionId(@PathVariable String transactionId) {
        return transactionApplicationService.getByTransactionId(transactionId);
    }

    @GetMapping("/reference/{referenceId}")
    public TransactionResponse getByReference(@PathVariable String referenceId) {
        return transactionApplicationService.getByReferenceId(referenceId);
    }
}
