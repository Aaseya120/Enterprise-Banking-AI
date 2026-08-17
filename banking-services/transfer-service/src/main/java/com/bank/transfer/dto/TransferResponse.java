package com.bank.transfer.dto;

import com.bank.transfer.domain.Transfer;
import com.bank.transfer.domain.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(
        String transferId,
        String sourceAccountId,
        String destinationAccountId,
        BigDecimal amount,
        String currency,
        TransferStatus status,
        Instant createdAt
) {
    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
                transfer.getTransferId(),
                transfer.getSourceAccountId(),
                transfer.getDestinationAccountId(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getStatus(),
                transfer.getCreatedAt()
        );
    }
}
