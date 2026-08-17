-- customer-service schema. Column definitions mirror com.bank.customer.domain.Customer exactly
-- (Flyway creates the schema; Hibernate's ddl-auto=validate then fails fast at startup if the
-- entity and this migration ever drift apart, instead of Hibernate silently altering the table).
--
-- national_id_encrypted holds AES-256-GCM ciphertext ONLY (see CryptoUtil in common-crypto and
-- CustomerService's encrypt-on-write / decrypt-on-read) -- never plaintext PII, even here.
CREATE TABLE customers (
    customer_id            VARCHAR(36)  NOT NULL PRIMARY KEY,
    full_name               VARCHAR(255) NOT NULL,
    email                   VARCHAR(255) NOT NULL,
    phone_number            VARCHAR(255) NOT NULL,
    kyc_status              VARCHAR(20)  NOT NULL,
    national_id_encrypted   VARCHAR(500),
    created_at              TIMESTAMP    NOT NULL,
    updated_at              TIMESTAMP    NOT NULL,
    CONSTRAINT uk_customers_email UNIQUE (email)
);

CREATE INDEX idx_customers_kyc_status ON customers (kyc_status);
