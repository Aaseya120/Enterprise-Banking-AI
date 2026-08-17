package com.bank.card.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, String> {
    List<Card> findByAccountId(String accountId);
    List<Card> findByCustomerId(String customerId);
}
