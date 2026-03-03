-- ============================================================
-- SAGA EXECUTIONS (Estado da Transação Distribuída)
-- ============================================================
CREATE TABLE saga_executions (
    id                  UUID PRIMARY KEY,
    saga_type           VARCHAR(50) NOT NULL,             
    status              VARCHAR(30) NOT NULL,                             
    payload             JSONB NOT NULL,                  
    idempotency_key     VARCHAR(100) UNIQUE NOT NULL,    
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ,
    failure_reason      TEXT,
    retry_count         INT NOT NULL DEFAULT 0,
    next_retry_at       TIMESTAMPTZ
);

CREATE INDEX idx_saga_status ON saga_executions(status) WHERE status NOT IN ('COMPLETED', 'COMPENSATED', 'FAILED');
CREATE INDEX idx_saga_idempotency ON saga_executions(idempotency_key);
CREATE INDEX idx_saga_retry ON saga_executions(next_retry_at) WHERE next_retry_at IS NOT NULL;

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

-- ============================================================
-- IDEMPOTENCY KEYS (Fallback se Redis estiver fora)
-- ============================================================
CREATE TABLE idempotency_keys (
    key             VARCHAR(100) PRIMARY KEY,
    saga_id         UUID NOT NULL,
    response_code   INT,
    response_body   JSONB,
    expires_at      TIMESTAMPTZ NOT NULL DEFAULT NOW() + INTERVAL '24 hours',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_idempotency_expires ON idempotency_keys(expires_at);

-- ============================================================
-- FUNÇÃO: Auto-atualiza updated_at
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_saga_updated_at
    BEFORE UPDATE ON saga_executions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
