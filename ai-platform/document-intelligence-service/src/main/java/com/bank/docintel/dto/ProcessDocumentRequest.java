package com.bank.docintel.dto;

import com.bank.docintel.domain.BankDocumentType;
import jakarta.validation.constraints.NotBlank;

public record ProcessDocumentRequest(
        @NotBlank String sourceRef,
        String text,
        BankDocumentType documentTypeHint
) {
}
