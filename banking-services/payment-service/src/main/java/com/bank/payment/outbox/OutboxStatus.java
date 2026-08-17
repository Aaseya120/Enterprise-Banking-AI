package com.bank.payment.outbox;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    DEAD_LETTER
}
