-- fraud-service schema: mirrors com.bank.fraud.domain.BlacklistEntry and
-- com.bank.fraud.domain.EvaluationLog.
CREATE TABLE blacklist_entries (
    id         VARCHAR(36)  NOT NULL PRIMARY KEY,
    entity_ref VARCHAR(255) NOT NULL,
    reason     VARCHAR(500) NOT NULL,
    added_at   TIMESTAMP    NOT NULL,
    CONSTRAINT uk_blacklist_entries_entity_ref UNIQUE (entity_ref)
);

CREATE TABLE evaluation_logs (
    id           VARCHAR(36)   NOT NULL PRIMARY KEY,
    account_id   VARCHAR(255)  NOT NULL,
    amount       NUMERIC(19,4) NOT NULL,
    decision     VARCHAR(20)   NOT NULL,
    evaluated_at TIMESTAMP     NOT NULL
);

-- Backs VelocityRule's countByAccountIdAndEvaluatedAtAfter query -- without
-- this index, the velocity check degrades to a full table scan per
-- evaluation as the log grows, which would defeat the point of a
-- sub-request fraud check.
CREATE INDEX idx_evaluation_logs_account_time ON evaluation_logs (account_id, evaluated_at);
