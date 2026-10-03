CREATE TABLE location (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID         NOT NULL REFERENCES tenant (id),
    name                VARCHAR(200) NOT NULL,
    line1               VARCHAR(200) NOT NULL,
    city                VARCHAR(120) NOT NULL,
    region              VARCHAR(80)  NOT NULL,
    postal_code         VARCHAR(20)  NOT NULL,
    country             CHAR(2)      NOT NULL DEFAULT 'US',
    time_zone           VARCHAR(64)  NOT NULL,
    jurisdictions       JSONB        NOT NULL DEFAULT '[]'::jsonb,
    wage_order          VARCHAR(32),
    deleted_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_location_tenant ON location (tenant_id);

CREATE TABLE person (
    id                      UUID PRIMARY KEY,
    tenant_id               UUID         NOT NULL REFERENCES tenant (id),
    external_employee_code  VARCHAR(64)  NOT NULL,
    legal_name              VARCHAR(200) NOT NULL,
    email                   VARCHAR(255),
    hire_date               DATE         NOT NULL,
    termination_date        DATE,
    exemption_status        VARCHAR(32)  NOT NULL,
    deleted_at              TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, external_employee_code)
);

CREATE INDEX idx_person_tenant ON person (tenant_id);

CREATE TABLE job_code (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenant (id),
    code            VARCHAR(32)  NOT NULL,
    name            VARCHAR(120) NOT NULL,
    UNIQUE (tenant_id, code)
);

CREATE TABLE assignment (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenant (id),
    person_id       UUID         NOT NULL REFERENCES person (id),
    location_id     UUID         NOT NULL REFERENCES location (id),
    job_code_id     UUID         NOT NULL REFERENCES job_code (id),
    effective_from  DATE         NOT NULL,
    effective_to    DATE
);

CREATE INDEX idx_assignment_person ON assignment (tenant_id, person_id);

CREATE TABLE pay_rate (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenant (id),
    assignment_id   UUID         NOT NULL REFERENCES assignment (id),
    rate_type       VARCHAR(32)  NOT NULL,
    amount          NUMERIC(12, 4) NOT NULL,
    effective_from  DATE         NOT NULL,
    effective_to    DATE
);

CREATE TABLE compensation_plan (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenant (id),
    person_id       UUID         NOT NULL REFERENCES person (id),
    plan_type       VARCHAR(48)  NOT NULL,
    percent         NUMERIC(8, 4),
    flat_amount     NUMERIC(12, 4),
    discretionary   BOOLEAN      NOT NULL DEFAULT false,
    effective_from  DATE         NOT NULL,
    effective_to    DATE
);

CREATE TABLE tenant_policy (
    tenant_id                   UUID PRIMARY KEY REFERENCES tenant (id),
    workweek_start              VARCHAR(16) NOT NULL DEFAULT 'SUNDAY',
    punch_round_minutes         INTEGER     NOT NULL DEFAULT 1,
    meal_waiver_under_six_hours BOOLEAN     NOT NULL DEFAULT true,
    auto_rest_premium           BOOLEAN     NOT NULL DEFAULT true,
    attestation_overrides_clock BOOLEAN     NOT NULL DEFAULT true,
    pay_period_type             VARCHAR(16) NOT NULL DEFAULT 'BIWEEKLY',
    export_destination          VARCHAR(32) NOT NULL DEFAULT 'GUSTO'
);
