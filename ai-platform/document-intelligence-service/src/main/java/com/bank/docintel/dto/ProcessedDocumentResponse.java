package com.bank.docintel.dto;

import com.bank.docintel.domain.BankDocumentType;
import com.bank.docintel.domain.ProcessedDocument;
import com.bank.docintel.domain.ReviewStatus;

import java.time.Instant;
import java.util.Map;

public record ProcessedDocumentResponse(
        String id,
        String sourceRef,
        BankDocumentType documentType,
        Map<String, String> extractedFields,
        double confidence,
        ReviewStatus reviewStatus,
        String reviewedBy,
        Instant createdAt
) {
    public static ProcessedDocumentResponse from(ProcessedDocument document, Map<String, String> fields) {
        return new ProcessedDocumentResponse(document.getId(), document.getSourceRef(), document.getDocumentType(),
                fields, document.getConfidence(), document.getReviewStatus(), document.getReviewedBy(),
                document.getCreatedAt());
    }
}
