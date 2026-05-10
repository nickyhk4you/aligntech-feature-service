CREATE TABLE IF NOT EXISTS feature_flags (
    id              UUID PRIMARY KEY,
    flag_key        VARCHAR(255) NOT NULL UNIQUE,
    name            VARCHAR(500) NOT NULL,
    description     TEXT,
    flag_type       VARCHAR(50) NOT NULL DEFAULT 'boolean',
    status          VARCHAR(50) NOT NULL DEFAULT 'draft',
    environments    JSONB NOT NULL DEFAULT '["development", "staging", "production"]',
    owner_team      VARCHAR(255),
    created_by      VARCHAR(255) NOT NULL,
    release_version VARCHAR(100),
    rollout         JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    activated_at    TIMESTAMPTZ,
    archived_at     TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS audit_log (
    id              UUID PRIMARY KEY,
    flag_id         UUID NOT NULL,
    action          VARCHAR(50) NOT NULL,
    changed_by      VARCHAR(255) NOT NULL,
    change_summary  TEXT,
    old_value       JSONB,
    new_value       JSONB,
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_feature_flags_status ON feature_flags(status);
CREATE INDEX IF NOT EXISTS idx_feature_flags_owner_team ON feature_flags(owner_team);
CREATE INDEX IF NOT EXISTS idx_feature_flags_flag_key ON feature_flags(flag_key);
CREATE INDEX IF NOT EXISTS idx_feature_flags_updated_at ON feature_flags(updated_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_flag_id ON audit_log(flag_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_changed_at ON audit_log(changed_at);
