package com.bank.audit.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Deliberately has no setters/mutators after construction -- an audit trail
 * that could be edited after the fact isn't an audit trail (plan section 34).
 * AuditEventService additionally redacts any accidental sensitive-field
 * names before this is ever constructed.
 */
@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String auditId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String resource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditResult result;

    private String ipAddress;
    private String correlationId;
    private String details;

    @Column(nullable = false)
    private Instant timestamp;

    protected AuditEvent() {
        // JPA
    }

    public AuditEvent(String userId, String action, String resource, AuditResult result,
                       String ipAddress, String correlationId, String details) {
        this.userId = userId;
        this.action = action;
        this.resource = resource;
        this.result = result;
        this.ipAddress = ipAddress;
        this.correlationId = correlationId;
        this.details = details;
        this.timestamp = Instant.now();
    }

    public String getAuditId() { return auditId; }
    public String getUserId() { return userId; }
    public String getAction() { return action; }
    public String getResource() { return resource; }
    public AuditResult getResult() { return result; }
    public String getIpAddress() { return ipAddress; }
    public String getCorrelationId() { return correlationId; }
    public String getDetails() { return details; }
    public Instant getTimestamp() { return timestamp; }
}
