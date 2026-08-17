package com.bank.fraud.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "blacklist_entries", uniqueConstraints = @UniqueConstraint(columnNames = "entityRef"))
public class BlacklistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String entityRef;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private Instant addedAt;

    protected BlacklistEntry() {
        // JPA
    }

    public BlacklistEntry(String entityRef, String reason) {
        this.entityRef = entityRef;
        this.reason = reason;
        this.addedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getEntityRef() { return entityRef; }
    public String getReason() { return reason; }
    public Instant getAddedAt() { return addedAt; }
}
