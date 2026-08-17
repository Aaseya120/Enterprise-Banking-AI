package com.bank.docintel.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "processed_documents")
public class ProcessedDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String sourceRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BankDocumentType documentType;

    /** JSON-encoded key/value structured fields extracted for this document type. */
    @Lob
    @Column(nullable = false)
    private String extractedFieldsJson;

    @Column(nullable = false)
    private double confidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus reviewStatus;

    private String reviewedBy;
    private String reviewNotes;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant reviewedAt;

    protected ProcessedDocument() {
        // JPA
    }

    public ProcessedDocument(String sourceRef, BankDocumentType documentType, String extractedFieldsJson,
                              double confidence, double autoApproveThreshold) {
        this.sourceRef = sourceRef;
        this.documentType = documentType;
        this.extractedFieldsJson = extractedFieldsJson;
        this.confidence = confidence;
        this.reviewStatus = confidence >= autoApproveThreshold
                ? ReviewStatus.AUTO_APPROVED : ReviewStatus.PENDING_REVIEW;
        this.createdAt = Instant.now();
    }

    public void review(boolean approved, String reviewedBy, String notes) {
        this.reviewStatus = approved ? ReviewStatus.APPROVED : ReviewStatus.REJECTED;
        this.reviewedBy = reviewedBy;
        this.reviewNotes = notes;
        this.reviewedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getSourceRef() { return sourceRef; }
    public BankDocumentType getDocumentType() { return documentType; }
    public String getExtractedFieldsJson() { return extractedFieldsJson; }
    public double getConfidence() { return confidence; }
    public ReviewStatus getReviewStatus() { return reviewStatus; }
    public String getReviewedBy() { return reviewedBy; }
    public String getReviewNotes() { return reviewNotes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReviewedAt() { return reviewedAt; }
}
