package com.aligntech.dataplane.dto;

import com.aligntech.dataplane.engine.EvaluationResult;
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
public class EvaluationResponse {

    private boolean success;
    private Instant evaluatedAt;
    private List<FlagEval> flags;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlagEval {
        private String flagKey;
        private boolean enabled;
        private String variant;
        private Object payload;
        private String matchedRuleName;
        private String reason;
        // explainability
        private String userId;
        private String region;
        private String releaseVersion;
    }

    public static EvaluationResponse from(List<EvaluationResult> results) {
        List<FlagEval> flags = results.stream()
                .map(r -> FlagEval.builder()
                        .flagKey(r.getFlagKey())
                        .enabled(r.isEnabled())
                        .variant(r.getVariant())
                        .payload(r.getPayload())
                        .matchedRuleName(r.getMatchedRuleName())
                        .reason(r.getReason())
                        .userId(r.getUserId())
                        .region(r.getRegion())
                        .releaseVersion(r.getReleaseVersion())
                        .build())
                .toList();

        return EvaluationResponse.builder()
                .success(true)
                .evaluatedAt(Instant.now())
                .flags(flags)
                .build();
    }
}
