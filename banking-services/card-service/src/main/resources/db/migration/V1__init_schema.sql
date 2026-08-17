-- card-service schema, mirrors com.bank.card.domain.Card. Note there is
-- deliberately no column for a full PAN anywhere in this table -- see
-- Card.java's javadoc: the full number is generated, returned once to the
-- caller, and never persisted.
CREATE TABLE cards (
    card_id           VARCHAR(36)  NOT NULL PRIMARY KEY,
    account_id        VARCHAR(255) NOT NULL,
    customer_id       VARCHAR(255) NOT NULL,
    masked_pan        VARCHAR(30)  NOT NULL,
    last4             VARCHAR(4)   NOT NULL,
    card_type         VARCHAR(20)  NOT NULL,
    status            VARCHAR(30)  NOT NULL,
    expiry_month_year VARCHAR(10)  NOT NULL,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL
);

CREATE INDEX idx_cards_account_id ON cards (account_id);
CREATE INDEX idx_cards_customer_id ON cards (customer_id);
