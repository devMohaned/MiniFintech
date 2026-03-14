CREATE TABLE wallet.transfer_transactions (
    id UUID PRIMARY KEY,
    source_wallet_id UUID NOT NULL,
    destination_wallet_id UUID NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    CONSTRAINT fk_transfer_source_wallet FOREIGN KEY (source_wallet_id) REFERENCES wallet_accounts(id),
    CONSTRAINT fk_transfer_destination_wallet FOREIGN KEY (destination_wallet_id) REFERENCES wallet_accounts(id),
    CONSTRAINT chk_transfer_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_transfer_wallets_different CHECK (source_wallet_id <> destination_wallet_id)
);

CREATE INDEX idx_transfer_source_wallet_id ON wallet.transfer_transactions(source_wallet_id);
CREATE INDEX idx_transfer_destination_wallet_id ON wallet.transfer_transactions(destination_wallet_id);
CREATE INDEX idx_transfer_created_at ON wallet.transfer_transactions(created_at);