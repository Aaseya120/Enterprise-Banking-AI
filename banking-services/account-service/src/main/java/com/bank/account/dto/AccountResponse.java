package com.bank.account.dto;

import com.bank.account.domain.Account;
import com.bank.account.domain.AccountStatus;
import com.bank.account.domain.AccountType;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        String accountId,
        String customerId,
        String accountNumber,
        AccountType accountType,
        AccountStatus status,
        BigDecimal balance,
        String currency,
        Instant createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getAccountId(),
                account.getCustomerId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getStatus(),
                account.getBalance(),
                account.getCurrency(),
                account.getCreatedAt()
        );
    }
}
