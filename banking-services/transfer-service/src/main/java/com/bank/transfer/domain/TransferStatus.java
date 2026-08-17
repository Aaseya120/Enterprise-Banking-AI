package com.bank.transfer.domain;

/**
 * Saga states from architecture plan section 24:
 * INITIATED -> PENDING_FRAUD_REVIEW -> COMPLETED
 *                                  \-> FAILED -> compensation (reversal) handled
 *                                                by whichever service already
 *                                                debited (out of scope for this
 *                                                scaffold's account-service, which
 *                                                is not wired to transfer-service yet)
 */
public enum TransferStatus {
    INITIATED,
    PENDING_FRAUD_REVIEW,
    COMPLETED,
    FAILED
}
