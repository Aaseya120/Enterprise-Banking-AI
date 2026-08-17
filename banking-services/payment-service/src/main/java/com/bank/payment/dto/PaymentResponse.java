package com.bank.payment.dto;

import com.bank.payment.domain.Payment;
import com.bank.payment.domain.PaymentStatus;
import com.bank.payment.domain.PaymentType;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        String paymentId,
        String paymentReference,
        PaymentType paymentType,
        String sourceAccountId,
        String destinationRef,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String failureReason,
        Instant createdAt,
        Instant processedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getPaymentId(), payment.getPaymentReference(), payment.getPaymentType(),
                payment.getSourceAccountId(), payment.getDestinationRef(), payment.getAmount(),
                payment.getCurrency(), payment.getStatus(), payment.getFailureReason(),
                payment.getCreatedAt(), payment.getProcessedAt());
    }
}
