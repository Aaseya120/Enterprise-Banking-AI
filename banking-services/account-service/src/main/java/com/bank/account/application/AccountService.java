package com.bank.account.application;

import com.bank.account.domain.Account;
import com.bank.account.domain.AccountRepository;
import com.bank.account.dto.AccountResponse;
import com.bank.account.dto.AdjustBalanceRequest;
import com.bank.account.dto.BalanceResponse;
import com.bank.account.dto.OpenAccountRequest;
import com.bank.common.events.AuditEventPublisher;
import com.bank.common.exception.BusinessException;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Business logic layer (section 41-42). Controllers stay thin and only
 * translate HTTP <-> service calls; all account business rules live here.
 * This is also the class that the MCP account-tool ultimately calls into
 * (indirectly, via this service's own REST API) — the AI layer never talks
 * to AccountRepository/the database directly (section 16).
 *
 * <p>Audit emission (gap filled): ACCOUNT_OPENED, ACCOUNT_DEBITED, and
 * ACCOUNT_CREDITED are published to banking.audit.events on every write.
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AuditEventPublisher auditPublisher;

    public AccountService(AccountRepository accountRepository,
                           AuditEventPublisher auditPublisher) {
        this.accountRepository = accountRepository;
        this.auditPublisher = auditPublisher;
    }

    @Transactional
    public AccountResponse openAccount(OpenAccountRequest request) {
        String accountNumber = generateAccountNumber();
        Account account = new Account(
                request.customerId(),
                accountNumber,
                request.accountType(),
                request.openingBalance(),
                request.currency()
        );
        account = accountRepository.save(account);
        auditPublisher.publish(
                MDC.get("userId"), "ACCOUNT_OPENED",
                "Account/" + account.getAccountId(),
                true, "account-service",
                Map.of("customerId", request.customerId(),
                        "accountType", request.accountType().name(),
                        "currency", request.currency()));
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountId) {
        return AccountResponse.from(findOrThrow(accountId));
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String accountId) {
        Account account = findOrThrow(accountId);
        return new BalanceResponse(account.getAccountId(), account.getBalance(), account.getCurrency());
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsForCustomer(String customerId) {
        return accountRepository.findByCustomerId(customerId).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional
    public AccountResponse debit(String accountId, AdjustBalanceRequest request) {
        Account account = findOrThrow(accountId);
        try {
            account.debit(request.amount());
        } catch (IllegalStateException e) {
            auditPublisher.publish(
                    MDC.get("userId"), "ACCOUNT_DEBIT_FAILED",
                    "Account/" + accountId,
                    false, "account-service",
                    Map.of("amount", request.amount(), "reason", e.getMessage()));
            throw BusinessException.ruleViolation(e.getMessage());
        }
        account = accountRepository.save(account);
        auditPublisher.publish(
                MDC.get("userId"), "ACCOUNT_DEBITED",
                "Account/" + accountId,
                true, "account-service",
                Map.of("amount", request.amount(), "balance", account.getBalance()));
        return AccountResponse.from(account);
    }

    @Transactional
    public AccountResponse credit(String accountId, AdjustBalanceRequest request) {
        Account account = findOrThrow(accountId);
        account.credit(request.amount());
        account = accountRepository.save(account);
        auditPublisher.publish(
                MDC.get("userId"), "ACCOUNT_CREDITED",
                "Account/" + accountId,
                true, "account-service",
                Map.of("amount", request.amount(), "balance", account.getBalance()));
        return AccountResponse.from(account);
    }

    private Account findOrThrow(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> BusinessException.notFound("Account not found: " + accountId));
    }

    private String generateAccountNumber() {
        return String.valueOf(1000000000L + Math.abs(UUID.randomUUID().getMostSignificantBits() % 8999999999L));
    }
}
