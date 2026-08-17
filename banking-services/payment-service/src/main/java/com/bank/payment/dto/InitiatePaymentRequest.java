package com.bank.payment.dto;

import com.bank.payment.domain.PaymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InitiatePaymentRequest(
        @NotBlank String sourceAccountId,
        @NotNull PaymentType paymentType,
        @NotBlank String destinationRef,
        String destinationBank,
        String destinationIfsc,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank String currency,
        String remarks
) {
}
