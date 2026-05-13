-- Add owner_team column for per-flag permissions
ALTER TABLE feature_flags
ADD COLUMN IF NOT EXISTS owner_team VARCHAR(255);

-- Create index for owner_team lookups
CREATE INDEX IF NOT EXISTS idx_feature_flags_owner_team ON feature_flags(owner_team);

-- Add comment
COMMENT ON COLUMN feature_flags.owner_team IS 'Team that owns this flag for permission control';
