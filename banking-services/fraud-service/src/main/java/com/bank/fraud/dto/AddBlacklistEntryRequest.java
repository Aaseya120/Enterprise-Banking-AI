package com.bank.fraud.dto;

import jakarta.validation.constraints.NotBlank;

public record AddBlacklistEntryRequest(@NotBlank String entityRef, @NotBlank String reason) {
}
