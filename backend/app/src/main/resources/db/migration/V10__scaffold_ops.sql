CREATE TABLE idempotency_record (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID         NOT NULL REFERENCES tenant (id),
    idempotency_key     VARCHAR(128) NOT NULL,
    method              VARCHAR(16)  NOT NULL,
    path                VARCHAR(255) NOT NULL,
    request_hash        VARCHAR(64)  NOT NULL,
    status_code         INTEGER      NOT NULL,
    response_body       TEXT         NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX idx_idempotency_tenant ON idempotency_record (tenant_id, created_at);
