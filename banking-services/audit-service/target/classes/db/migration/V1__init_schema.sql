-- audit-service schema, mirrors com.bank.audit.domain.AuditEvent. There is
-- deliberately no UPDATE-supporting column set beyond what AuditEvent
-- exposes -- the entity has no mutators after construction (append-only,
-- see its javadoc), and this table has no reason to be updated either.
CREATE TABLE audit_events (
    audit_id       VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id        VARCHAR(255) NOT NULL,
    action         VARCHAR(100) NOT NULL,
    resource       VARCHAR(255) NOT NULL,
    result         VARCHAR(20)  NOT NULL,
    ip_address     VARCHAR(50),
    correlation_id VARCHAR(100),
    details        VARCHAR(2000),
    timestamp      TIMESTAMP    NOT NULL
);

CREATE INDEX idx_audit_events_user_timestamp ON audit_events (user_id, timestamp DESC);
CREATE INDEX idx_audit_events_resource_timestamp ON audit_events (resource, timestamp DESC);
