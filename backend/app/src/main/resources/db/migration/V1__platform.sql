CREATE TABLE tenant (
    id              UUID PRIMARY KEY,
    legal_name      VARCHAR(200) NOT NULL,
    slug            VARCHAR(80)  NOT NULL UNIQUE,
    status          VARCHAR(32)  NOT NULL,
    plan            VARCHAR(32)  NOT NULL,
    vertical        VARCHAR(32)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE platform_user (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenant (id),
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(200) NOT NULL,
    role            VARCHAR(32)  NOT NULL,
    email_verified  BOOLEAN      NOT NULL DEFAULT false,
    mfa_enabled     BOOLEAN      NOT NULL DEFAULT false,
    invited_by      UUID         REFERENCES platform_user (id),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_login_at   TIMESTAMPTZ,
    UNIQUE (tenant_id, email)
);

CREATE INDEX idx_platform_user_tenant ON platform_user (tenant_id);

CREATE TABLE refresh_token (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES platform_user (id),
    tenant_id       UUID         NOT NULL REFERENCES tenant (id),
    token_hash      VARCHAR(64)  NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ  NOT NULL,
    revoked_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE subscription (
    id                      UUID PRIMARY KEY,
    tenant_id               UUID         NOT NULL UNIQUE REFERENCES tenant (id),
    stripe_customer_id      VARCHAR(64),
    stripe_subscription_id  VARCHAR(64),
    plan                    VARCHAR(32)  NOT NULL,
    status                  VARCHAR(32)  NOT NULL,
    current_period_end      TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE tenant_entitlement (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenant (id),
    code            VARCHAR(64)  NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT true,
    UNIQUE (tenant_id, code)
);

CREATE TABLE feature_flag (
    code            VARCHAR(64) PRIMARY KEY,
    enabled         BOOLEAN      NOT NULL DEFAULT false,
    note            VARCHAR(255)
);
