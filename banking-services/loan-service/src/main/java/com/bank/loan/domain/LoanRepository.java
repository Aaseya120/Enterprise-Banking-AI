package com.bank.loan.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, String> {
    List<Loan> findByCustomerId(String customerId);
    Page<Loan> findByStatus(LoanStatus status, Pageable pageable);
    Page<Loan> findByCustomerIdAndStatus(String customerId, LoanStatus status, Pageable pageable);
}
