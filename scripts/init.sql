SET default_transaction_isolation TO 'serializable';
-- ============================================================
-- DOMÍNIO: CONTAS
-- ============================================================
CREATE TABLE accounts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id        UUID NOT NULL,
    account_number  VARCHAR(20) UNIQUE NOT NULL,
    balance         NUMERIC(19, 4) NOT NULL DEFAULT 0,  
    currency        CHAR(3) NOT NULL DEFAULT 'BRL',
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED')),
    version         BIGINT NOT NULL DEFAULT 0,            -- optimistic locking
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_balance_non_negative CHECK (balance >= 0)
    -- saldo nunca pode ser negativo no banco.
    -- qualquer tentativa de UPDATE que resulte em saldo < 0 é abortada pelo DB
);

CREATE INDEX idx_accounts_owner ON accounts(owner_id);
CREATE INDEX idx_accounts_number ON accounts(account_number);
-- ============================================================
-- DOMÍNIO: SAGA EXECUTIONS (Estado da Transação Distribuída)
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
-- AUDIT LOG IMUTÁVEL (Append-Only)
-- ============================================================
CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,               
    saga_id         UUID,                                
    event_type      VARCHAR(50) NOT NULL,
    account_id      UUID,
    amount          NUMERIC(19, 4),
    balance_before  NUMERIC(19, 4),
    balance_after   NUMERIC(19, 4),
    currency        CHAR(3),
    metadata        JSONB,                               
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    checksum        VARCHAR(64) NOT NULL                 
);

REVOKE UPDATE, DELETE ON audit_log FROM ledger_user;
CREATE INDEX idx_audit_saga ON audit_log(saga_id);
CREATE INDEX idx_audit_account ON audit_log(account_id, occurred_at DESC);
CREATE INDEX idx_audit_type ON audit_log(event_type, occurred_at DESC);
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

CREATE TRIGGER trg_accounts_updated_at
    BEFORE UPDATE ON accounts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_saga_updated_at
    BEFORE UPDATE ON saga_executions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
-- ============================================================
-- DADOS INICIAIS DE TESTE
-- ============================================================
INSERT INTO accounts (id, owner_id, account_number, balance, currency)
VALUES
    ('a1000000-0000-0000-0000-000000000001', gen_random_uuid(), 'ACC-0001', 10000.0000, 'BRL'),
    ('a2000000-0000-0000-0000-000000000002', gen_random_uuid(), 'ACC-0002', 5000.0000,  'BRL');