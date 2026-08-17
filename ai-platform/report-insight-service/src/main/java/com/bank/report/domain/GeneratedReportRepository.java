package com.bank.report.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneratedReportRepository extends JpaRepository<GeneratedReport, String> {
    Page<GeneratedReport> findBySubjectRefOrderByCreatedAtDesc(String subjectRef, Pageable pageable);
}
