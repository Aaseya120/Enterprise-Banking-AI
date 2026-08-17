package com.bank.docintel.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedDocumentRepository extends JpaRepository<ProcessedDocument, String> {
    Page<ProcessedDocument> findByReviewStatus(ReviewStatus status, Pageable pageable);
}
