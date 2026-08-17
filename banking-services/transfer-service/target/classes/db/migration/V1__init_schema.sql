-- transfer-service schema: Transfer aggregate + its Transactional Outbox
-- (mirrors com.bank.transfer.domain.Transfer and com.bank.transfer.outbox.OutboxEvent).
CREATE TABLE transfers (
    transfer_id             VARCHAR(36)   NOT NULL PRIMARY KEY,
    source_account_id       VARCHAR(255)  NOT NULL,
    destination_account_id  VARCHAR(255)  NOT NULL,
    amount                  NUMERIC(19,4) NOT NULL,
    currency                VARCHAR(10)   NOT NULL,
    status                  VARCHAR(30)   NOT NULL,
    created_at              TIMESTAMP     NOT NULL,
    updated_at              TIMESTAMP     NOT NULL
);

CREATE TABLE outbox_events (
    id             VARCHAR(36)  NOT NULL PRIMARY KEY,
    aggregate_id   VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        TEXT         NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    retry_count    INTEGER      NOT NULL,
    created_at     TIMESTAMP    NOT NULL,
    published_at   TIMESTAMP
);

-- OutboxPublisher polls PENDING rows ordered by age -- this index is what
-- keeps that poll cheap as the table grows instead of a full scan.
CREATE INDEX idx_outbox_events_status_created ON outbox_events (status, created_at);
