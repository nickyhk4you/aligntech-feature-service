package com.aligntech.evaluation.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationContext {

    private String userId;
    private String tenantId;
    private String region;
    private String appVersion;
    private String platform;
    private String appKey;

    @Builder.Default
    private Map<String, Object> customAttributes = new HashMap<>();

    public String getAttribute(String key) {
        return switch (key) {
            case "user_id" -> userId;
            case "tenant_id" -> tenantId;
            case "region" -> region;
            case "app_version" -> appVersion;
            case "platform" -> platform;
            default -> {
                Object val = customAttributes.get(key);
                yield val != null ? val.toString() : null;
            }
        };
    }
}
