package com.bank.knowledge.dto;

import jakarta.validation.constraints.NotBlank;

public record PublishNewVersionRequest(
        @NotBlank String title,
        @NotBlank String content,
        String storageLocation,
        @NotBlank String createdBy
) {
}
