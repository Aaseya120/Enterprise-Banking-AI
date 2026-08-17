-- document-intelligence-service schema, mirrors com.bank.docintel.domain.ProcessedDocument.
CREATE TABLE processed_documents (
    id                     VARCHAR(36)   NOT NULL PRIMARY KEY,
    source_ref             VARCHAR(500)  NOT NULL,
    document_type          VARCHAR(30)   NOT NULL,
    extracted_fields_json  TEXT          NOT NULL,
    confidence             DOUBLE PRECISION NOT NULL,
    review_status          VARCHAR(20)   NOT NULL,
    reviewed_by            VARCHAR(255),
    review_notes           VARCHAR(1000),
    created_at             TIMESTAMP     NOT NULL,
    reviewed_at            TIMESTAMP
);

-- Backs the review-queue endpoint's actual query (findByReviewStatus).
CREATE INDEX idx_processed_documents_review_status ON processed_documents (review_status);
