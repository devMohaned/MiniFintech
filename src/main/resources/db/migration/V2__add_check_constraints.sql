ALTER TABLE wallet.wallet_accounts
    ADD CONSTRAINT chk_wallet_balance_non_negative CHECK (available_balance >= 0);

ALTER TABLE wallet.ledger_entries
    ADD CONSTRAINT chk_ledger_amount_positive CHECK (amount > 0);