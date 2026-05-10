package com.aligntech.sdk;

import java.util.HashMap;
import java.util.Map;

/**
 * User/entity context for feature flag evaluation.
 * Mirrors dataplane EvaluationContext but without Spring dependencies.
 */
public class AlignTechUser {

    private final String userId;
    private final String tenantId;
    private final String region;
    private final String appVersion;
    private final String platform;
    private final Map<String, Object> custom;

    private AlignTechUser(Builder builder) {
        this.userId = builder.userId;
        this.tenantId = builder.tenantId;
        this.region = builder.region;
        this.appVersion = builder.appVersion;
        this.platform = builder.platform;
        this.custom = Map.copyOf(builder.custom);
    }

    public String getUserId() { return userId; }
    public String getTenantId() { return tenantId; }
    public String getRegion() { return region; }
    public String getAppVersion() { return appVersion; }
    public String getPlatform() { return platform; }

    public Object getAttribute(String key) {
        if (custom.containsKey(key)) return custom.get(key);
        return switch (key) {
            case "user_id" -> userId;
            case "tenant_id" -> tenantId;
            case "region" -> region;
            case "app_version" -> appVersion;
            case "platform" -> platform;
            default -> null;
        };
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String userId;
        private String tenantId;
        private String region;
        private String appVersion;
        private String platform;
        private final Map<String, Object> custom = new HashMap<>();

        public Builder userId(String v) { this.userId = v; return this; }
        public Builder tenantId(String v) { this.tenantId = v; return this; }
        public Builder region(String v) { this.region = v; return this; }
        public Builder appVersion(String v) { this.appVersion = v; return this; }
        public Builder platform(String v) { this.platform = v; return this; }
        public Builder attr(String key, Object value) { this.custom.put(key, value); return this; }

        public AlignTechUser build() { return new AlignTechUser(this); }
    }
}
