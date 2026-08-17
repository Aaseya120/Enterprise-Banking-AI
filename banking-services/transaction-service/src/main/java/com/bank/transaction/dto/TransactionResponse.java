package com.bank.transaction.dto;

import com.bank.transaction.domain.TransactionRecord;
import com.bank.transaction.domain.TransactionRecordStatus;
import com.bank.transaction.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        String transactionId,
        String accountId,
        String referenceId,
        String referenceType,
        TransactionType transactionType,
        BigDecimal amount,
        String currency,
        TransactionRecordStatus status,
        String description,
        Instant createdAt
) {
    public static TransactionResponse from(TransactionRecord record) {
        return new TransactionResponse(
                record.getTransactionId(), record.getAccountId(), record.getReferenceId(),
                record.getReferenceType(), record.getTransactionType(), record.getAmount(),
                record.getCurrency(), record.getStatus(), record.getDescription(), record.getCreatedAt());
    }
}
