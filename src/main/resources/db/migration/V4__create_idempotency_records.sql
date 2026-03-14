CREATE TABLE wallet.idempotency_records (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    resource_id VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    response_code INTEGER,
    response_body TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_idempotency_key_operation UNIQUE (idempotency_key, operation_type)
);

CREATE INDEX idx_idempotency_created_at ON wallet.idempotency_records(created_at);
CREATE INDEX idx_idempotency_resource_id ON wallet.idempotency_records(resource_id);