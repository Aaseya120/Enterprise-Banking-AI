package com.bank.knowledge.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * One immutable row per version of a document's content (plan section 21:
 * "Support document versioning"). KnowledgeDocument holds the current
 * pointer + mutable metadata (owner, classification, status); this table
 * is the append-only history -- publishing a new version never edits an
 * existing row, only adds one and repoints KnowledgeDocument.currentVersion.
 */
@Entity
@Table(name = "document_versions")
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String documentId;

    @Column(nullable = false)
    private int versionNumber;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private String storageLocation;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    protected DocumentVersion() {
        // JPA
    }

    public DocumentVersion(String documentId, int versionNumber, String title, String content,
                            String storageLocation, String createdBy) {
        this.documentId = documentId;
        this.versionNumber = versionNumber;
        this.title = title;
        this.content = content;
        this.storageLocation = storageLocation;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getDocumentId() { return documentId; }
    public int getVersionNumber() { return versionNumber; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getStorageLocation() { return storageLocation; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
