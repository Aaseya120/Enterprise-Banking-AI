-- knowledge-service schema: mirrors com.bank.knowledge.domain.KnowledgeDocument
-- (current pointer + mutable metadata) and com.bank.knowledge.domain.DocumentVersion
-- (immutable append-only version history).
CREATE TABLE knowledge_documents (
    document_id      VARCHAR(36)  NOT NULL PRIMARY KEY,
    title            VARCHAR(500) NOT NULL,
    document_type    VARCHAR(30)  NOT NULL,
    current_version  INTEGER      NOT NULL,
    owner            VARCHAR(255) NOT NULL,
    department       VARCHAR(255) NOT NULL,
    classification   VARCHAR(20)  NOT NULL,
    access_roles     VARCHAR(500) NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    effective_date   TIMESTAMP,
    expiration_date  TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL
);

CREATE TABLE document_versions (
    id               VARCHAR(36)  NOT NULL PRIMARY KEY,
    document_id      VARCHAR(255) NOT NULL,
    version_number   INTEGER      NOT NULL,
    title            VARCHAR(500) NOT NULL,
    content          TEXT         NOT NULL,
    storage_location VARCHAR(500) NOT NULL,
    created_by       VARCHAR(255) NOT NULL,
    created_at       TIMESTAMP    NOT NULL,
    CONSTRAINT uk_document_versions_doc_version UNIQUE (document_id, version_number)
);

CREATE INDEX idx_knowledge_documents_type ON knowledge_documents (document_type);
CREATE INDEX idx_knowledge_documents_status ON knowledge_documents (status);
CREATE INDEX idx_document_versions_document_id ON document_versions (document_id);
