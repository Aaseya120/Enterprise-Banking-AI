package com.bank.transaction.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, String> {
    Page<TransactionRecord> findByAccountIdOrderByCreatedAtDesc(String accountId, Pageable pageable);
    Optional<TransactionRecord> findByTransactionId(String transactionId);
    Optional<TransactionRecord> findByReferenceId(String referenceId);
    boolean existsByReferenceId(String referenceId);
}
