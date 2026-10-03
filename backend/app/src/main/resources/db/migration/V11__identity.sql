ALTER TABLE platform_user ADD COLUMN location_id UUID REFERENCES location (id);
ALTER TABLE platform_user ADD COLUMN totp_secret VARCHAR(64);
ALTER TABLE platform_user ADD COLUMN totp_confirmed BOOLEAN NOT NULL DEFAULT false;

CREATE TABLE user_invite (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenant (id),
    email           VARCHAR(255) NOT NULL,
    role            VARCHAR(32)  NOT NULL,
    location_id     UUID         REFERENCES location (id),
    token_hash      VARCHAR(64)  NOT NULL UNIQUE,
    invited_by      UUID         NOT NULL REFERENCES platform_user (id),
    expires_at      TIMESTAMPTZ  NOT NULL,
    accepted_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE email_verification (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES platform_user (id),
    token_hash      VARCHAR(64)  NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ  NOT NULL,
    consumed_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
