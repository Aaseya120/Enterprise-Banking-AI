package com.bank.transfer.outbox;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    DEAD_LETTER
}
