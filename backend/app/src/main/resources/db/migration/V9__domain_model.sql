-- Step 4 domain freeze. If a table does not support a payroll run, it is not in v1.
-- geo_claim is deferred to v1.1.

ALTER TABLE location ADD COLUMN opening_hours JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE person ADD COLUMN worker_type VARCHAR(16) NOT NULL DEFAULT 'EMPLOYEE';
ALTER TABLE platform_user ADD COLUMN user_kind VARCHAR(16) NOT NULL DEFAULT 'TENANT';

CREATE TABLE employment (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID         NOT NULL REFERENCES tenant (id),
    person_id           UUID         NOT NULL REFERENCES person (id),
    worker_type         VARCHAR(16)  NOT NULL,
    exemption_status    VARCHAR(32)  NOT NULL,
    hire_date           DATE         NOT NULL,
    termination_date    DATE,
    effective_from      DATE         NOT NULL,
    effective_to        DATE
);

CREATE INDEX idx_employment_person ON employment (tenant_id, person_id, effective_from);

CREATE TABLE leave_policy (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID         NOT NULL REFERENCES tenant (id),
    leave_type          VARCHAR(24)  NOT NULL,
    accrual_method      VARCHAR(32)  NOT NULL,
    accrual_rate        NUMERIC(8, 4) NOT NULL DEFAULT 0,
    cap_hours           NUMERIC(8, 4),
    carryover_hours     NUMERIC(8, 4),
    state_overlay       VARCHAR(8),
    effective_from      DATE         NOT NULL,
    effective_to        DATE
);

CREATE INDEX idx_leave_policy_tenant ON leave_policy (tenant_id);

CREATE INDEX idx_assignment_location ON assignment (tenant_id, location_id);

ALTER TABLE earnings_line ADD COLUMN pay_period_id UUID REFERENCES pay_period (id);
ALTER TABLE earnings_line ADD COLUMN reversing_of UUID REFERENCES earnings_line (id);

CREATE INDEX idx_earnings_period ON earnings_line (tenant_id, pay_period_id);

UPDATE earnings_line e
SET pay_period_id = r.pay_period_id
FROM pay_run r
WHERE e.pay_run_id = r.id
  AND e.pay_period_id IS NULL;

COMMENT ON TABLE punch IS 'Never DELETE. Void with voided_at plus a timesheet_edit row.';
COMMENT ON TABLE earnings_line IS 'Never DELETE. Void with a reversing line (reversing_of).';
COMMENT ON TABLE timesheet_edit IS 'Append-only. Never UPDATE a punch in place without a compensating row here.';
COMMENT ON TABLE person IS 'Soft delete via deleted_at only. Unique (tenant_id, external_employee_code) for clock import.';
COMMENT ON TABLE location IS 'Soft delete via deleted_at only. opening_hours (IANA-local) for split-shift logic.';
COMMENT ON TABLE pay_rate IS 'Effective dated. A Wednesday raise must not rewrite Monday.';
COMMENT ON TABLE assignment IS 'Effective dated person + location + job_code.';
COMMENT ON TABLE employment IS 'A person can have multiple employment stints; rates hang off assignment, not this row.';
COMMENT ON TABLE leave_policy IS 'Accrual method, cap, carryover, optional state overlay. Engine use is Step 12.';
COMMENT ON INDEX idx_punch_person_time IS 'Day-one index: (tenant_id, person_id, timestamp).';
COMMENT ON INDEX idx_earnings_period IS 'Day-one index: (tenant_id, pay_period_id) on earnings.';
COMMENT ON INDEX idx_assignment_location IS 'Day-one index: (tenant_id, location_id) on assignments.';
