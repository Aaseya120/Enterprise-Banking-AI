package com.bank.account.controller;

import com.bank.account.application.AccountService;
import com.bank.account.dto.AccountResponse;
import com.bank.account.dto.AdjustBalanceRequest;
import com.bank.account.dto.BalanceResponse;
import com.bank.account.dto.OpenAccountRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * HTTP concerns only (section 42) -- validation annotations + delegation.
 * This is the API the MCP account-tool and the API Gateway route to; the AI
 * orchestrator never bypasses it to reach the database directly.
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> openAccount(@Valid @RequestBody OpenAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.openAccount(request));
    }

    @GetMapping("/{accountId}")
    public AccountResponse getAccount(@PathVariable String accountId) {
        return accountService.getAccount(accountId);
    }

    @GetMapping("/{accountId}/balance")
    public BalanceResponse getBalance(@PathVariable String accountId) {
        return accountService.getBalance(accountId);
    }

    @GetMapping(params = "customerId")
    public List<AccountResponse> getAccountsForCustomer(@RequestParam String customerId) {
        return accountService.getAccountsForCustomer(customerId);
    }

    @PostMapping("/{accountId}/debit")
    public AccountResponse debit(@PathVariable String accountId, @Valid @RequestBody AdjustBalanceRequest request) {
        return accountService.debit(accountId, request);
    }

    @PostMapping("/{accountId}/credit")
    public AccountResponse credit(@PathVariable String accountId, @Valid @RequestBody AdjustBalanceRequest request) {
        return accountService.credit(accountId, request);
    }
}
