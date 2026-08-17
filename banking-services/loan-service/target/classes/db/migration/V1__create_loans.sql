CREATE TABLE loans (
    loan_id               VARCHAR(36)    PRIMARY KEY,
    customer_id           VARCHAR(36)    NOT NULL,
    account_id            VARCHAR(36)    NOT NULL,
    loan_type             VARCHAR(20)    NOT NULL,
    principal_amount      NUMERIC(19,4)  NOT NULL,
    outstanding_balance   NUMERIC(19,4)  NOT NULL,
    annual_interest_rate  NUMERIC(7,4)   NOT NULL,
    tenure_months         INTEGER        NOT NULL,
    emi                   NUMERIC(19,4)  NOT NULL DEFAULT 0,
    currency              VARCHAR(3)     NOT NULL,
    status                VARCHAR(20)    NOT NULL,
    purpose               TEXT           NOT NULL,
    rejection_reason      TEXT,
    applied_at            TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    approved_at           TIMESTAMPTZ,
    disbursed_at          TIMESTAMPTZ,
    closed_at             TIMESTAMPTZ,
    next_payment_due      DATE
);

CREATE INDEX idx_loans_customer  ON loans(customer_id);
CREATE INDEX idx_loans_status    ON loans(status);
