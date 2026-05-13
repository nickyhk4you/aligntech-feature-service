package com.aligntech.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplainResponse {

    private String flagKey;
    private boolean enabled;
    private String reason;
    private Instant evaluatedAt;
    
    private FlagContext flagContext;
    private UserContext userContext;
    private RolloutExplanation rolloutExplanation;
    private List<String> applicableRules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlagContext {
        private String status;
        private String flagType;
        private String releaseVersion;
        private List<String> environments;
        private Instant activatedAt;
        private String createdBy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserContext {
        private String userId;
        private String region;
        private String platform;
        private String appVersion;
        private Map<String, Object> customAttributes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RolloutExplanation {
        private String rolloutType;
        private Map<String, Object> rolloutConfig;
        private String matchReason;
        private boolean matched;
    }
}
