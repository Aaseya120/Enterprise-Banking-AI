package com.bank.fraud.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One row per POST /evaluate call. VelocityRule counts rows for the same
 * accountId within a trailing window to catch rapid-fire transaction
 * attempts -- a classic fraud signal that a single-transaction threshold
 * check alone would miss.
 */
@Entity
@Table(name = "evaluation_logs")
public class EvaluationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private String decision;

    @Column(nullable = false)
    private Instant evaluatedAt;

    protected EvaluationLog() {
        // JPA
    }

    public EvaluationLog(String accountId, BigDecimal amount, String decision) {
        this.accountId = accountId;
        this.amount = amount;
        this.decision = decision;
        this.evaluatedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getAccountId() { return accountId; }
    public BigDecimal getAmount() { return amount; }
    public String getDecision() { return decision; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
}
