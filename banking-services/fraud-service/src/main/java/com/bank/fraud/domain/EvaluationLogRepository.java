package com.bank.fraud.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface EvaluationLogRepository extends JpaRepository<EvaluationLog, String> {
    long countByAccountIdAndEvaluatedAtAfter(String accountId, Instant since);
}
