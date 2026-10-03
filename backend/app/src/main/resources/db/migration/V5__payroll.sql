CREATE TABLE pay_period (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenant (id),
    period_type     VARCHAR(16)  NOT NULL,
    start_date      DATE         NOT NULL,
    end_date        DATE         NOT NULL,
    status          VARCHAR(24)  NOT NULL,
    UNIQUE (tenant_id, start_date, end_date)
);

CREATE TABLE pay_run (
    id                      UUID PRIMARY KEY,
    tenant_id               UUID         NOT NULL REFERENCES tenant (id),
    pay_period_id           UUID         NOT NULL REFERENCES pay_period (id),
    status                  VARCHAR(32)  NOT NULL,
    engine_version          VARCHAR(32)  NOT NULL,
    rule_pack_versions      JSONB        NOT NULL DEFAULT '[]'::jsonb,
    created_by              UUID         REFERENCES platform_user (id),
    approved_by             UUID         REFERENCES platform_user (id),
    approved_at             TIMESTAMPTZ,
    approval_ip             VARCHAR(64),
    unlocked_by             UUID         REFERENCES platform_user (id),
    unlocked_at             TIMESTAMPTZ,
    snapshot_sha256         VARCHAR(64),
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE earnings_line (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenant (id),
    pay_run_id      UUID         NOT NULL REFERENCES pay_run (id),
    person_id       UUID         NOT NULL REFERENCES person (id),
    work_date       DATE,
    bucket          VARCHAR(32)  NOT NULL,
    hours           NUMERIC(8, 4)  NOT NULL DEFAULT 0,
    rate            NUMERIC(12, 4) NOT NULL DEFAULT 0,
    amount          NUMERIC(12, 4) NOT NULL,
    explanation     JSONB        NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX idx_earnings_run ON earnings_line (tenant_id, pay_run_id);

CREATE TABLE pay_run_exception (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenant (id),
    pay_run_id      UUID         NOT NULL REFERENCES pay_run (id),
    person_id       UUID         REFERENCES person (id),
    work_date       DATE,
    exception_type  VARCHAR(48)  NOT NULL,
    severity        VARCHAR(16)  NOT NULL,
    blocker         BOOLEAN      NOT NULL DEFAULT false,
    message         VARCHAR(500) NOT NULL,
    dismissed       BOOLEAN      NOT NULL DEFAULT false,
    dismiss_reason  VARCHAR(500)
);

CREATE TABLE export_file (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenant (id),
    pay_run_id      UUID         NOT NULL REFERENCES pay_run (id),
    destination     VARCHAR(32)  NOT NULL,
    checksum_sha256 VARCHAR(64)  NOT NULL,
    content         TEXT         NOT NULL,
    generated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE pay_run_snapshot (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenant (id),
    pay_run_id      UUID         NOT NULL UNIQUE REFERENCES pay_run (id),
    payload         JSONB        NOT NULL,
    sha256          VARCHAR(64)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE bonus_entry (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenant (id),
    person_id       UUID         NOT NULL REFERENCES person (id),
    amount          NUMERIC(12, 4) NOT NULL,
    earned_on       DATE         NOT NULL,
    discretionary   BOOLEAN      NOT NULL DEFAULT false,
    note            VARCHAR(255)
);
