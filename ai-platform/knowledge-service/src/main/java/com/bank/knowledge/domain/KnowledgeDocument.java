package com.bank.knowledge.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "knowledge_documents")
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String documentId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    @Column(nullable = false)
    private int currentVersion;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentClassification classification;

    /** Comma-separated role names allowed to view this document, mirrors rag-service's chunk-level "roles" metadata filter. */
    @Column(nullable = false)
    private String accessRoles;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    private Instant effectiveDate;
    private Instant expirationDate;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected KnowledgeDocument() {
        // JPA
    }

    public KnowledgeDocument(String title, DocumentType documentType, String owner, String department,
                              DocumentClassification classification, String accessRoles,
                              Instant effectiveDate, Instant expirationDate) {
        this.title = title;
        this.documentType = documentType;
        this.currentVersion = 1;
        this.owner = owner;
        this.department = department;
        this.classification = classification;
        this.accessRoles = accessRoles;
        this.status = DocumentStatus.DRAFT;
        this.effectiveDate = effectiveDate;
        this.expirationDate = expirationDate;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void publish() {
        this.status = DocumentStatus.ACTIVE;
        touch();
    }

    public void retire() {
        this.status = DocumentStatus.RETIRED;
        touch();
    }

    public int publishNewVersion(String title) {
        this.currentVersion++;
        this.title = title;
        this.status = DocumentStatus.ACTIVE;
        touch();
        return this.currentVersion;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public String getDocumentId() { return documentId; }
    public String getTitle() { return title; }
    public DocumentType getDocumentType() { return documentType; }
    public int getCurrentVersion() { return currentVersion; }
    public String getOwner() { return owner; }
    public String getDepartment() { return department; }
    public DocumentClassification getClassification() { return classification; }
    public String getAccessRoles() { return accessRoles; }
    public DocumentStatus getStatus() { return status; }
    public Instant getEffectiveDate() { return effectiveDate; }
    public Instant getExpirationDate() { return expirationDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
