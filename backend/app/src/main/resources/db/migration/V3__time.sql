CREATE TABLE punch_import_batch (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenant (id),
    file_name       VARCHAR(255) NOT NULL,
    file_sha256     VARCHAR(64)  NOT NULL,
    row_count       INTEGER      NOT NULL DEFAULT 0,
    status          VARCHAR(32)  NOT NULL,
    imported_by     UUID         REFERENCES platform_user (id),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, file_sha256)
);

CREATE TABLE punch (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID         NOT NULL REFERENCES tenant (id),
    person_id           UUID         NOT NULL REFERENCES person (id),
    location_id         UUID         NOT NULL REFERENCES location (id),
    import_batch_id     UUID         REFERENCES punch_import_batch (id),
    punch_type          VARCHAR(24)  NOT NULL,
    source              VARCHAR(24)  NOT NULL,
    original_at         TIMESTAMPTZ  NOT NULL,
    adjusted_at         TIMESTAMPTZ  NOT NULL,
    adjust_reason       VARCHAR(32),
    adjust_note         VARCHAR(500),
    adjusted_by         UUID         REFERENCES platform_user (id),
    voided_at           TIMESTAMPTZ
);

CREATE INDEX idx_punch_person_time ON punch (tenant_id, person_id, adjusted_at);

CREATE TABLE timesheet_edit (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenant (id),
    punch_id        UUID         NOT NULL REFERENCES punch (id),
    actor_id        UUID         NOT NULL REFERENCES platform_user (id),
    reason_code     VARCHAR(32)  NOT NULL,
    note            VARCHAR(500),
    before_json     JSONB        NOT NULL,
    after_json      JSONB        NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE meal_attestation (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenant (id),
    person_id       UUID         NOT NULL REFERENCES person (id),
    work_date       DATE         NOT NULL,
    meal_received   VARCHAR(16)  NOT NULL,
    rest_received   VARCHAR(16)  NOT NULL,
    source          VARCHAR(24)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
