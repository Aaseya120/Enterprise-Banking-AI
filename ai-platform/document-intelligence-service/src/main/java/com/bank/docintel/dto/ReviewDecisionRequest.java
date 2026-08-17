package com.bank.docintel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReviewDecisionRequest(@NotNull Boolean approved, @NotBlank String reviewedBy, String notes) {
}
