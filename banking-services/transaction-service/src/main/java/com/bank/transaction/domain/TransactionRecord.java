package com.bank.transaction.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * An immutable ledger entry (plan section F1). Rows are never updated in
 * place after creation except to move status PENDING -> COMPLETED/REVERSED
 * -- balanceBefore/balanceAfter are a point-in-time snapshot recorded when
 * the row is written, not recomputed later.
 */
@Entity
@Table(name = "transaction_records")
public class TransactionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String transactionId;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String referenceId;

    @Column(nullable = false)
    private String referenceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionRecordStatus status;

    private String description;
    private String channel;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant valueDate;

    protected TransactionRecord() {
        // JPA
    }

    public TransactionRecord(String accountId, String referenceId, String referenceType,
                              TransactionType transactionType, BigDecimal amount, String currency,
                              String description, String channel) {
        this.transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.accountId = accountId;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.transactionType = transactionType;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.channel = channel;
        this.status = TransactionRecordStatus.COMPLETED;
        this.createdAt = Instant.now();
        this.valueDate = Instant.now();
    }

    public String getId() { return id; }
    public String getTransactionId() { return transactionId; }
    public String getAccountId() { return accountId; }
    public String getReferenceId() { return referenceId; }
    public String getReferenceType() { return referenceType; }
    public TransactionType getTransactionType() { return transactionType; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public TransactionRecordStatus getStatus() { return status; }
    public String getDescription() { return description; }
    public String getChannel() { return channel; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getValueDate() { return valueDate; }
}
