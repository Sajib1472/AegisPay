CREATE TABLE leave_ledger (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenant (id),
    person_id       UUID         NOT NULL REFERENCES person (id),
    pay_run_id      UUID         REFERENCES pay_run (id),
    policy_code     VARCHAR(32)  NOT NULL,
    entry_type      VARCHAR(16)  NOT NULL,
    hours_delta     NUMERIC(8, 4) NOT NULL,
    balance_after   NUMERIC(8, 4) NOT NULL,
    work_date       DATE,
    note            VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_leave_ledger_person ON leave_ledger (tenant_id, person_id, created_at);

CREATE TABLE leave_balance (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenant (id),
    person_id       UUID         NOT NULL REFERENCES person (id),
    policy_code     VARCHAR(32)  NOT NULL,
    hours           NUMERIC(8, 4) NOT NULL,
    as_of           DATE         NOT NULL,
    UNIQUE (tenant_id, person_id, policy_code)
);
