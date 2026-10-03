CREATE SCHEMA IF NOT EXISTS platform;

ALTER TABLE feature_flag SET SCHEMA platform;

ALTER TABLE rule_pack SET SCHEMA platform;
ALTER TABLE rule_pack_document SET SCHEMA platform;

ALTER TABLE pay_run ADD COLUMN rule_pack_ids JSONB NOT NULL DEFAULT '[]'::jsonb;

COMMENT ON COLUMN earnings_line.amount IS 'NUMERIC(12,4) money freeze; never float/double';
COMMENT ON COLUMN earnings_line.hours IS 'NUMERIC(8,4) hours freeze; 0.25 = 15 minutes';
COMMENT ON COLUMN punch.adjusted_at IS 'timestamptz UTC; convert to location IANA zone before daily OT';
