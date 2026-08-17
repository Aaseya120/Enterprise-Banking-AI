-- transaction-service schema: the immutable ledger (mirrors
-- com.bank.transaction.domain.TransactionRecord). reference_id is unique
-- because TransactionApplicationService.recordFromEvent() uses
-- existsByReferenceId as its idempotent-consumer check.
CREATE TABLE transaction_records (
    id               VARCHAR(36)   NOT NULL PRIMARY KEY,
    transaction_id   VARCHAR(255)  NOT NULL,
    account_id       VARCHAR(255)  NOT NULL,
    reference_id     VARCHAR(255)  NOT NULL,
    reference_type   VARCHAR(50)   NOT NULL,
    transaction_type VARCHAR(20)   NOT NULL,
    amount           NUMERIC(19,4) NOT NULL,
    currency         VARCHAR(10)   NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    description      VARCHAR(500),
    channel          VARCHAR(50),
    created_at       TIMESTAMP     NOT NULL,
    value_date       TIMESTAMP     NOT NULL,
    CONSTRAINT uk_transaction_records_transaction_id UNIQUE (transaction_id),
    CONSTRAINT uk_transaction_records_reference_id UNIQUE (reference_id)
);

-- Backs TransactionRecordRepository.findByAccountIdOrderByCreatedAtDesc,
-- the statement-query endpoint's actual access pattern.
CREATE INDEX idx_transaction_records_account_created ON transaction_records (account_id, created_at DESC);
