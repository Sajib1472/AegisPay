ALTER TABLE tenant_policy
    ADD COLUMN lock_on_export BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE pay_run
    ADD COLUMN exported_at TIMESTAMPTZ;

ALTER TABLE pay_run
    ADD COLUMN lock_at TIMESTAMPTZ;

ALTER TABLE pay_run_exception
    ADD COLUMN dismissed_by UUID REFERENCES platform_user (id);

ALTER TABLE pay_run_exception
    ADD COLUMN dismissed_at TIMESTAMPTZ;

ALTER TABLE bonus_entry
    ADD COLUMN pay_period_id UUID REFERENCES pay_period (id);

CREATE TABLE pay_run_unlock_request (
    id            UUID PRIMARY KEY,
    tenant_id     UUID         NOT NULL REFERENCES tenant (id),
    pay_run_id    UUID         NOT NULL REFERENCES pay_run (id),
    requested_by  UUID         NOT NULL REFERENCES platform_user (id),
    requested_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    confirmed_by  UUID         REFERENCES platform_user (id),
    confirmed_at  TIMESTAMPTZ,
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING'
);

CREATE INDEX idx_unlock_run ON pay_run_unlock_request (tenant_id, pay_run_id);
