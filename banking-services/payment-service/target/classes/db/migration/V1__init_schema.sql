-- payment-service schema: Payment aggregate + Transactional Outbox
-- (mirrors com.bank.payment.domain.Payment and com.bank.payment.outbox.OutboxEvent).
CREATE TABLE payments (
    payment_id         VARCHAR(36)   NOT NULL PRIMARY KEY,
    payment_reference  VARCHAR(255)  NOT NULL,
    idempotency_key    VARCHAR(255)  NOT NULL,
    payment_type       VARCHAR(20)   NOT NULL,
    source_account_id  VARCHAR(255)  NOT NULL,
    destination_ref    VARCHAR(255)  NOT NULL,
    destination_bank   VARCHAR(255),
    destination_ifsc   VARCHAR(255),
    amount             NUMERIC(19,4) NOT NULL,
    currency           VARCHAR(10)   NOT NULL,
    status             VARCHAR(20)   NOT NULL,
    remarks            VARCHAR(500),
    failure_reason     VARCHAR(500),
    version            BIGINT        NOT NULL,
    created_at         TIMESTAMP     NOT NULL,
    processed_at       TIMESTAMP,
    CONSTRAINT uk_payments_reference UNIQUE (payment_reference),
    CONSTRAINT uk_payments_idempotency_key UNIQUE (idempotency_key)
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

CREATE INDEX idx_outbox_events_status_created ON outbox_events (status, created_at);
CREATE INDEX idx_payments_source_account ON payments (source_account_id);
