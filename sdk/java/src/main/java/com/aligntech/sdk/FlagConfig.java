package com.aligntech.sdk;

import java.util.Collections;
import java.util.Map;


public class FlagConfig {

    private final String flagKey;
    private final String flagType;
    private final String status;
    private final Map<String, Object> rollout;

    public FlagConfig(String flagKey, String flagType, String status, Map<String, Object> rollout) {
        this.flagKey = flagKey;
        this.flagType = flagType;
        this.status = status;
        this.rollout = rollout != null ? Collections.unmodifiableMap(rollout) : Collections.emptyMap();
    }

    public String getFlagKey() { return flagKey; }
    public String getFlagType() { return flagType; }
    public String getStatus() { return status; }
    public Map<String, Object> getRollout() { return rollout; }

    public boolean isActive() { return "active".equals(status); }
}
