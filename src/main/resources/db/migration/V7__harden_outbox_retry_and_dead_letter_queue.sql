ALTER TABLE wallet.outbox_events
    ADD COLUMN next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE wallet.dead_letter_events (
    id UUID PRIMARY KEY,
    original_event_id UUID NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    payload_json TEXT NOT NULL,
    attempts INTEGER NOT NULL,
    last_error_message TEXT,
    dead_lettered_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_dead_letter_original_event_id ON wallet.dead_letter_events(original_event_id);
CREATE INDEX idx_dead_letter_dead_lettered_at ON wallet.dead_letter_events(dead_lettered_at);
CREATE INDEX idx_outbox_next_attempt_at ON wallet.outbox_events(next_attempt_at);