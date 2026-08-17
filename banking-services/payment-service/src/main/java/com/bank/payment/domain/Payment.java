package com.bank.payment.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments", uniqueConstraints = @UniqueConstraint(columnNames = "idempotencyKey"))
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String paymentId;

    @Column(nullable = false, unique = true)
    private String paymentReference;

    /**
     * Deduplication key (plan section D1/14): a retried request with the
     * same key returns the already-processed Payment instead of creating a
     * second one -- see PaymentApplicationService.initiatePayment.
     */
    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType;

    @Column(nullable = false)
    private String sourceAccountId;

    @Column(nullable = false)
    private String destinationRef;

    private String destinationBank;
    private String destinationIfsc;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String remarks;
    private String failureReason;

    @Version
    private long version;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant processedAt;

    protected Payment() {
        // JPA
    }

    public Payment(String idempotencyKey, PaymentType paymentType, String sourceAccountId,
                    String destinationRef, String destinationBank, String destinationIfsc,
                    BigDecimal amount, String currency, String remarks) {
        this.paymentReference = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.idempotencyKey = idempotencyKey;
        this.paymentType = paymentType;
        this.sourceAccountId = sourceAccountId;
        this.destinationRef = destinationRef;
        this.destinationBank = destinationBank;
        this.destinationIfsc = destinationIfsc;
        this.amount = amount;
        this.currency = currency;
        this.remarks = remarks;
        this.status = PaymentStatus.INITIATED;
        this.createdAt = Instant.now();
    }

    public void authorize() {
        this.status = PaymentStatus.AUTHORIZED;
    }

    public void markProcessing() {
        this.status = PaymentStatus.PROCESSING;
    }

    public void markCompleted() {
        this.status = PaymentStatus.COMPLETED;
        this.processedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.processedAt = Instant.now();
    }

    public void markReversed() {
        this.status = PaymentStatus.REVERSED;
    }

    public String getPaymentId() { return paymentId; }
    public String getPaymentReference() { return paymentReference; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public PaymentType getPaymentType() { return paymentType; }
    public String getSourceAccountId() { return sourceAccountId; }
    public String getDestinationRef() { return destinationRef; }
    public String getDestinationBank() { return destinationBank; }
    public String getDestinationIfsc() { return destinationIfsc; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public String getRemarks() { return remarks; }
    public String getFailureReason() { return failureReason; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getProcessedAt() { return processedAt; }
}
