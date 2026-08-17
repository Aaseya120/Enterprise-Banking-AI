package com.bank.knowledge.dto;

import com.bank.knowledge.domain.DocumentClassification;
import com.bank.knowledge.domain.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record CreateDocumentRequest(
        @NotBlank String title,
        @NotNull DocumentType documentType,
        @NotBlank String owner,
        @NotBlank String department,
        @NotNull DocumentClassification classification,
        List<String> accessRoles,
        @NotBlank String content,
        String storageLocation,
        @NotBlank String createdBy,
        Instant effectiveDate,
        Instant expirationDate
) {
}
