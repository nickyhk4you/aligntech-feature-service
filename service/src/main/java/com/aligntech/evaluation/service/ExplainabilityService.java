package com.aligntech.evaluation.service;

import com.aligntech.domain.FeatureFlag;
import com.aligntech.evaluation.dto.ExplainRequest;
import com.aligntech.evaluation.dto.ExplainResponse;
import com.aligntech.evaluation.engine.EvaluationContext;
import com.aligntech.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExplainabilityService {

    private final FeatureFlagRepository flagRepo;

    public ExplainResponse explain(String flagKey, ExplainRequest request) {
        FeatureFlag flag = flagRepo.findByFlagKey(flagKey)
                .orElseThrow(() -> new IllegalArgumentException("Flag not found: " + flagKey));

        EvaluationContext context = buildContext(request);
        
        ExplainResponse.FlagContext flagContext = ExplainResponse.FlagContext.builder()
                .status(flag.getStatus())
                .flagType(flag.getFlagType())
                .releaseVersion(flag.getReleaseVersion())
                .environments(flag.getEnvironments())
                .activatedAt(flag.getActivatedAt())
                .createdBy(flag.getCreatedBy())
                .build();

        ExplainResponse.UserContext userContext = ExplainResponse.UserContext.builder()
                .userId(request.getUserId())
                .region(request.getRegion())
                .platform(request.getPlatform())
                .appVersion(request.getAppVersion())
                .customAttributes(request.getCustomAttributes())
                .build();

        ExplainResponse.ExplainResponseBuilder response = ExplainResponse.builder()
                .flagKey(flagKey)
                .evaluatedAt(Instant.now())
                .flagContext(flagContext)
                .userContext(userContext);

        if (!flag.isActive()) {
            return response
                    .enabled(false)
                    .reason("Flag is not active (status: " + flag.getStatus() + ")")
                    .rolloutExplanation(ExplainResponse.RolloutExplanation.builder()
                            .rolloutType("N/A")
                            .matched(false)
                            .matchReason("Flag status is " + flag.getStatus())
                            .build())
                    .applicableRules(Collections.emptyList())
                    .build();
        }

        Map<String, Object> rollout = flag.getRollout();
        if (rollout == null || rollout.isEmpty()) {
            return response
                    .enabled(false)
                    .reason("No rollout configuration defined")
                    .rolloutExplanation(ExplainResponse.RolloutExplanation.builder()
                            .rolloutType("none")
                            .matched(false)
                            .matchReason("No rollout rules configured")
                            .build())
                    .applicableRules(Collections.emptyList())
                    .build();
        }

        String type = (String) rollout.getOrDefault("type", "boolean");
        ExplainResponse.RolloutExplanation rolloutExplanation = explainRollout(type, rollout, context);

        return response
                .enabled(rolloutExplanation.isMatched())
                .reason(rolloutExplanation.getMatchReason())
                .rolloutExplanation(rolloutExplanation)
                .applicableRules(getApplicableRules(type, rollout, context))
                .build();
    }

    private ExplainResponse.RolloutExplanation explainRollout(String type, Map<String, Object> rollout, EvaluationContext context) {
        ExplainResponse.RolloutExplanation.ExplainResponseRolloutExplanationBuilder explanation = 
                ExplainResponse.RolloutExplanation.builder()
                        .rolloutType(type)
                        .rolloutConfig(rollout);

        switch (type) {
            case "boolean":
                boolean value = Boolean.TRUE.equals(rollout.get("value"));
                return explanation
                        .matched(value)
                        .matchReason(value ? "Boolean flag is enabled for all users" : "Boolean flag is disabled for all users")
                        .build();

            case "whitelist":
                @SuppressWarnings("unchecked")
                List<String> userIds = (List<String>) rollout.get("userIds");
                if (userIds == null || userIds.isEmpty()) {
                    return explanation
                            .matched(false)
                            .matchReason("Whitelist is empty")
                            .build();
                }
                
                String userId = context.getUserId();
                if (userId == null) {
                    return explanation
                            .matched(false)
                            .matchReason("User ID is not provided")
                            .build();
                }

                for (String pattern : userIds) {
                    if (matchesPattern(userId, pattern)) {
                        return explanation
                                .matched(true)
                                .matchReason("User ID '" + userId + "' matches whitelist pattern: " + pattern)
                                .build();
                    }
                }

                return explanation
                        .matched(false)
                        .matchReason("User ID '" + userId + "' does not match any whitelist patterns: " + userIds)
                        .build();

            case "percentage":
                int percentage = ((Number) rollout.getOrDefault("percentage", 0)).intValue();
                if (context.getUserId() == null) {
                    return explanation
                            .matched(false)
                            .matchReason("User ID required for percentage rollout")
                            .build();
                }
                
                int hash = Math.abs(context.getUserId().hashCode() % 100);
                boolean inPercentage = hash < percentage;
                
                return explanation
                        .matched(inPercentage)
                        .matchReason(String.format("User hash %d %s in rollout percentage %d%%", 
                                hash, inPercentage ? "is" : "is not", percentage))
                        .build();

            case "region":
                @SuppressWarnings("unchecked")
                List<String> allowedRegions = (List<String>) rollout.get("regions");
                if (allowedRegions == null || allowedRegions.isEmpty()) {
                    return explanation
                            .matched(false)
                            .matchReason("No regions configured")
                            .build();
                }
                
                String userRegion = context.getRegion();
                if (userRegion == null) {
                    return explanation
                            .matched(false)
                            .matchReason("User region is not provided")
                            .build();
                }

                boolean regionMatched = allowedRegions.contains(userRegion);
                return explanation
                        .matched(regionMatched)
                        .matchReason(regionMatched 
                                ? "User region '" + userRegion + "' is in allowed regions: " + allowedRegions
                                : "User region '" + userRegion + "' is not in allowed regions: " + allowedRegions)
                        .build();

            default:
                return explanation
                        .matched(false)
                        .matchReason("Unknown rollout type: " + type)
                        .build();
        }
    }

    private boolean matchesPattern(String value, String pattern) {
        if (pattern.equals("*")) return true;
        if (pattern.startsWith("*") && pattern.endsWith("*"))
            return value.contains(pattern.substring(1, pattern.length() - 1));
        else if (pattern.startsWith("*"))
            return value.endsWith(pattern.substring(1));
        else if (pattern.endsWith("*"))
            return value.startsWith(pattern.substring(0, pattern.length() - 1));
        else
            return pattern.equals(value);
    }

    private List<String> getApplicableRules(String type, Map<String, Object> rollout, EvaluationContext context) {
        List<String> rules = new ArrayList<>();
        
        rules.add("Flag must be in 'active' status");
        
        switch (type) {
            case "boolean":
                rules.add("Boolean value determines enablement for all users");
                break;
            case "whitelist":
                rules.add("User ID must match one of the whitelist patterns");
                @SuppressWarnings("unchecked")
                List<String> userIds = (List<String>) rollout.get("userIds");
                if (userIds != null) {
                    rules.add("Whitelist patterns: " + userIds);
                }
                break;
            case "percentage":
                int percentage = ((Number) rollout.getOrDefault("percentage", 0)).intValue();
                rules.add("User hash must be less than " + percentage + "%");
                rules.add("Hash is deterministic based on user ID");
                break;
            case "region":
                @SuppressWarnings("unchecked")
                List<String> regions = (List<String>) rollout.get("regions");
                if (regions != null) {
                    rules.add("User region must be one of: " + regions);
                }
                break;
        }
        
        return rules;
    }

    private EvaluationContext buildContext(ExplainRequest request) {
        return EvaluationContext.builder()
                .userId(request.getUserId())
                .tenantId(request.getTenantId())
                .region(request.getRegion())
                .appVersion(request.getAppVersion())
                .platform(request.getPlatform())
                .customAttributes(request.getCustomAttributes())
                .build();
    }
}
