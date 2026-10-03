CREATE TABLE audit_artifact (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenant (id),
    pay_run_id      UUID         NOT NULL UNIQUE REFERENCES pay_run (id),
    storage_key     VARCHAR(255) NOT NULL,
    sha256          VARCHAR(64)  NOT NULL,
    byte_length     INTEGER      NOT NULL,
    generated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

ALTER TABLE pay_run
    ADD COLUMN audit_pdf_sha256 VARCHAR(64);
