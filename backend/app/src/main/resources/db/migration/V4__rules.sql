CREATE TABLE rule_pack (
    id              UUID PRIMARY KEY,
    jurisdiction    VARCHAR(64)  NOT NULL,
    version         VARCHAR(32)  NOT NULL,
    effective_from  DATE         NOT NULL,
    effective_to    DATE,
    sha256          VARCHAR(64)  NOT NULL,
    status          VARCHAR(24)  NOT NULL,
    engine_module   VARCHAR(64)  NOT NULL,
    UNIQUE (jurisdiction, version)
);

CREATE TABLE rule_pack_document (
    id              UUID PRIMARY KEY,
    rule_pack_id    UUID         NOT NULL REFERENCES rule_pack (id),
    title           VARCHAR(200) NOT NULL,
    citation        VARCHAR(200) NOT NULL,
    body_markdown   TEXT         NOT NULL
);

INSERT INTO rule_pack (id, jurisdiction, version, effective_from, sha256, status, engine_module)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'US-FLSA', '2024.1', DATE '1938-06-25',
     'flsa-2024.1-placeholder-sha', 'PUBLISHED', 'FLSA'),
    ('22222222-2222-2222-2222-222222222222', 'US-CA', '2024.1', DATE '2024-01-01',
     'ca-2024.1-placeholder-sha', 'PUBLISHED', 'CALIFORNIA');
