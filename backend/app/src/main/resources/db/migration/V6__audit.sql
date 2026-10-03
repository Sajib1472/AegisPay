CREATE TABLE audit_event (
    id              UUID PRIMARY KEY,
    tenant_id       UUID,
    actor_id        UUID,
    action          VARCHAR(64)  NOT NULL,
    entity_type     VARCHAR(64)  NOT NULL,
    entity_id       VARCHAR(64),
    before_json     JSONB,
    after_json      JSONB,
    ip_address      VARCHAR(64),
    user_agent      VARCHAR(255),
    request_id      VARCHAR(64),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_tenant_time ON audit_event (tenant_id, created_at);
