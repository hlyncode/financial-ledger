CREATE TABLE accounts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id        UUID NOT NULL,
    account_number  VARCHAR(20) UNIQUE NOT NULL,
    balance         NUMERIC(19, 4) NOT NULL DEFAULT 0,  
    currency        CHAR(3) NOT NULL DEFAULT 'BRL',
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED')),
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_balance_non_negative CHECK (balance >= 0)
);

CREATE INDEX idx_accounts_owner ON accounts(owner_id);
CREATE INDEX idx_accounts_number ON accounts(account_number);
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

CREATE INDEX idx_audit_saga ON audit_log(saga_id);
CREATE INDEX idx_audit_account ON audit_log(account_id, occurred_at DESC);
CREATE INDEX idx_audit_type ON audit_log(event_type, occurred_at DESC);

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
