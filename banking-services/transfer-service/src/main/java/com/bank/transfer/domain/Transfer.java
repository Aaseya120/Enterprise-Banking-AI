package com.bank.transfer.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String transferId;

    @Column(nullable = false)
    private String sourceAccountId;

    @Column(nullable = false)
    private String destinationAccountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Transfer() {
        // JPA
    }

    public Transfer(String sourceAccountId, String destinationAccountId, BigDecimal amount, String currency) {
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.currency = currency;
        this.status = TransferStatus.INITIATED;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markPendingFraudReview() {
        this.status = TransferStatus.PENDING_FRAUD_REVIEW;
        this.updatedAt = Instant.now();
    }

    public void markCompleted() {
        this.status = TransferStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public void markFailed() {
        this.status = TransferStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    public String getTransferId() { return transferId; }
    public String getSourceAccountId() { return sourceAccountId; }
    public String getDestinationAccountId() { return destinationAccountId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public TransferStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
