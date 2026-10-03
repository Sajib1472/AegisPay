ALTER TABLE subscription
    ADD COLUMN grace_until TIMESTAMPTZ;

ALTER TABLE subscription
    ADD COLUMN annual BOOLEAN NOT NULL DEFAULT false;

CREATE TABLE stripe_event (
    id            VARCHAR(64) PRIMARY KEY,
    type          VARCHAR(64) NOT NULL,
    processed_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
