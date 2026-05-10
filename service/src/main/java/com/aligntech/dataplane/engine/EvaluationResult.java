package com.aligntech.dataplane.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResult {

    private String flagKey;
    private boolean enabled;
    private String variant;
    private Object payload;
    private UUID matchedRuleId;
    private String matchedRuleName;
    private String reason;
    // explainability fields
    private String userId;
    private String region;
    private String releaseVersion;
}
