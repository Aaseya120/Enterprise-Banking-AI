package com.bank.audit.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, String> {
    Page<AuditEvent> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);
    Page<AuditEvent> findByResourceOrderByTimestampDesc(String resource, Pageable pageable);
}
