package com.bank.account.dto;

import com.bank.account.domain.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OpenAccountRequest(
        @NotBlank String customerId,
        @NotNull AccountType accountType,
        @NotNull @DecimalMin(value = "0.0") BigDecimal openingBalance,
        @NotBlank String currency
) {
}
