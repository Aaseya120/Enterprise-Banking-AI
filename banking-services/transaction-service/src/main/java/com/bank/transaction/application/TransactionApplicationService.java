package com.bank.transaction.application;

import com.bank.common.exception.BusinessException;
import com.bank.transaction.domain.*;
import com.bank.transaction.dto.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Section F1/F3: records ledger entries. recordFromEvent() is the entry
 * point used by the Kafka consumer -- it is idempotent on referenceId (the
 * upstream payment/transfer id), so an at-least-once redelivery of the same
 * PAYMENT_COMPLETED/TRANSFER_COMPLETED event never creates a duplicate
 * ledger row (the idempotent-consumer requirement from plan section 13).
 */
@Service
public class TransactionApplicationService {

    private final TransactionRecordRepository transactionRecordRepository;

    public TransactionApplicationService(TransactionRecordRepository transactionRecordRepository) {
        this.transactionRecordRepository = transactionRecordRepository;
    }

    @Transactional
    public void recordFromEvent(String accountId, String referenceId, String referenceType,
                                 TransactionType type, BigDecimal amount, String currency,
                                 String description) {
        if (transactionRecordRepository.existsByReferenceId(referenceId)) {
            return; // already recorded -- idempotent no-op
        }
        TransactionRecord record = new TransactionRecord(
                accountId, referenceId, referenceType, type, amount, currency, description, "SYSTEM");
        transactionRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getStatement(String accountId, Pageable pageable) {
        return transactionRecordRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable)
                .map(TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getByTransactionId(String transactionId) {
        return transactionRecordRepository.findByTransactionId(transactionId)
                .map(TransactionResponse::from)
                .orElseThrow(() -> BusinessException.notFound("Transaction not found: " + transactionId));
    }

    @Transactional(readOnly = true)
    public TransactionResponse getByReferenceId(String referenceId) {
        return transactionRecordRepository.findByReferenceId(referenceId)
                .map(TransactionResponse::from)
                .orElseThrow(() -> BusinessException.notFound("No transaction for reference: " + referenceId));
    }
}
