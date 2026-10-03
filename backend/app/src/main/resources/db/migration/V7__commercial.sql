ALTER TABLE tenant ADD COLUMN trial_ends_at TIMESTAMPTZ;
ALTER TABLE tenant ADD COLUMN audit_pack_enabled BOOLEAN NOT NULL DEFAULT false;

UPDATE tenant
SET trial_ends_at = created_at + INTERVAL '14 days'
WHERE plan = 'PILOT' AND trial_ends_at IS NULL;
