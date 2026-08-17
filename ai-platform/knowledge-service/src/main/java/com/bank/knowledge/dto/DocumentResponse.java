package com.bank.knowledge.dto;

import com.bank.knowledge.domain.DocumentClassification;
import com.bank.knowledge.domain.DocumentStatus;
import com.bank.knowledge.domain.DocumentType;
import com.bank.knowledge.domain.DocumentVersion;
import com.bank.knowledge.domain.KnowledgeDocument;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public record DocumentResponse(
        String documentId,
        String title,
        DocumentType documentType,
        int currentVersion,
        String owner,
        String department,
        DocumentClassification classification,
        List<String> accessRoles,
        DocumentStatus status,
        Instant effectiveDate,
        Instant expirationDate,
        Instant updatedAt
) {
    public static DocumentResponse from(KnowledgeDocument doc) {
        List<String> roles = doc.getAccessRoles() == null || doc.getAccessRoles().isBlank()
                ? List.of() : Arrays.asList(doc.getAccessRoles().split(","));
        return new DocumentResponse(doc.getDocumentId(), doc.getTitle(), doc.getDocumentType(),
                doc.getCurrentVersion(), doc.getOwner(), doc.getDepartment(), doc.getClassification(),
                roles, doc.getStatus(), doc.getEffectiveDate(), doc.getExpirationDate(), doc.getUpdatedAt());
    }

    public record VersionResponse(String documentId, int versionNumber, String title, String content,
                                   String storageLocation, String createdBy, Instant createdAt) {
        public static VersionResponse from(DocumentVersion version) {
            return new VersionResponse(version.getDocumentId(), version.getVersionNumber(), version.getTitle(),
                    version.getContent(), version.getStorageLocation(), version.getCreatedBy(), version.getCreatedAt());
        }
    }
}
