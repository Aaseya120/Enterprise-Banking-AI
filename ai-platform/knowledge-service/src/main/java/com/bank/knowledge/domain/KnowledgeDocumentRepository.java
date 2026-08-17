package com.bank.knowledge.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, String> {
    Page<KnowledgeDocument> findByDocumentType(DocumentType documentType, Pageable pageable);
    Page<KnowledgeDocument> findByStatus(DocumentStatus status, Pageable pageable);
}
