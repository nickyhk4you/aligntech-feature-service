export interface AlignTechConfig {
  baseUrl?: string;
  appKey: string;
  pollInterval?: number;
}

export interface AlignTechUser {
  userId?: string;
  tenantId?: string;
  region?: string;
  appVersion?: string;
  platform?: string;
  custom?: Record<string, any>;
}

export interface FlagConfig {
  flagKey: string;
  flagType: string;
  status: string;
  rollout?: Record<string, any>;
}

export interface EvaluationResult {
  flagKey: string;
  enabled: boolean;
  variant?: string;
  payload?: any;
  reason: string;
}

export interface SnapshotResponse {
  version: string;
  flags: FlagConfig[];
}
