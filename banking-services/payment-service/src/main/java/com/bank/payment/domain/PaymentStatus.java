package com.bank.payment.domain;

public enum PaymentStatus {
    INITIATED,
    VALIDATING,
    AUTHORIZED,
    PROCESSING,
    COMPLETED,
    FAILED,
    REVERSED
}
