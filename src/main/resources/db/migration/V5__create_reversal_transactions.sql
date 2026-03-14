ALTER TABLE wallet.transfer_transactions ADD COLUMN reversed_at TIMESTAMP;


CREATE TABLE wallet.reversal_transactions (
    id UUID PRIMARY KEY,
    original_transfer_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    CONSTRAINT fk_reversal_original_transfer FOREIGN KEY (original_transfer_id) REFERENCES transfer_transactions(id),
    CONSTRAINT uk_reversal_original_transfer UNIQUE (original_transfer_id)
);

CREATE INDEX idx_reversal_original_transfer_id ON wallet.reversal_transactions(original_transfer_id);
CREATE INDEX idx_reversal_created_at ON wallet.reversal_transactions(created_at);

