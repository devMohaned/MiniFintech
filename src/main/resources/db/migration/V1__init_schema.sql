CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE SCHEMA IF NOT EXISTS wallet;

CREATE TABLE wallet.wallet_accounts (
    id UUID PRIMARY KEY,
    customer_id VARCHAR(100) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    available_balance NUMERIC(19, 4) NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_wallet_customer_currency UNIQUE (customer_id, currency)
);

CREATE TABLE wallet.ledger_entries (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    wallet_id UUID NOT NULL,
    entry_type VARCHAR(10) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    reference_type VARCHAR(30) NOT NULL,
    reference_id VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_ledger_wallet FOREIGN KEY (wallet_id) REFERENCES wallet_accounts(id)
);

CREATE INDEX idx_wallet_accounts_customer_id ON wallet.wallet_accounts(customer_id);
CREATE INDEX idx_ledger_entries_wallet_id ON wallet.ledger_entries(wallet_id);
CREATE INDEX idx_ledger_entries_transaction_id ON wallet.ledger_entries(transaction_id);