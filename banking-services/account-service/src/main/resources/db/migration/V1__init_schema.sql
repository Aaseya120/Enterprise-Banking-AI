-- account-service schema, mirrors com.bank.account.domain.Account.
CREATE TABLE accounts (
    account_id     VARCHAR(36)    NOT NULL PRIMARY KEY,
    customer_id    VARCHAR(255)   NOT NULL,
    account_number VARCHAR(255)   NOT NULL,
    account_type   VARCHAR(20)    NOT NULL,
    status         VARCHAR(20)    NOT NULL,
    balance        NUMERIC(19,4)  NOT NULL,
    currency       VARCHAR(10)    NOT NULL,
    created_at     TIMESTAMP      NOT NULL,
    updated_at     TIMESTAMP      NOT NULL,
    CONSTRAINT uk_accounts_account_number UNIQUE (account_number)
);

CREATE INDEX idx_accounts_customer_id ON accounts (customer_id);
