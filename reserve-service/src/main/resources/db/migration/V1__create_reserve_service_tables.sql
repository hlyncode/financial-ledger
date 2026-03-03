-- ============================================================
-- OUTBOX PATTERN (Garantia de entrega ao Kafka)
-- ============================================================
CREATE TABLE outbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    saga_id         UUID NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    topic           VARCHAR(100) NOT NULL,               
    partition_key   VARCHAR(100),                        
    payload         JSONB NOT NULL,
    published       BOOLEAN NOT NULL DEFAULT FALSE,
    published_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    attempts        INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_pending ON outbox_events(created_at ASC)
    WHERE published = FALSE;
