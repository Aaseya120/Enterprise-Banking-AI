package com.bank.fraud.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BlacklistEntryRepository extends JpaRepository<BlacklistEntry, String> {
    boolean existsByEntityRef(String entityRef);
}
