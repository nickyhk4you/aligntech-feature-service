package com.aligntech.evaluation.engine;

import com.aligntech.domain.FeatureFlag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuleEvaluatorTest {

    private RuleEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new RuleEvaluator();
    }

    @Test
    @DisplayName("Should return false when flag is inactive")
    void testInactiveFlag() {
        FeatureFlag flag = FeatureFlag.builder()
                .flagKey("test-flag")
                .name("Test Flag")
                .status("draft")
                .createdBy("test")
                .build();

        EvaluationContext context = EvaluationContext.builder()
                .userId("user-123")
                .build();

        EvaluationResult result = evaluator.evaluate(flag, context);

        assertFalse(result.isEnabled());
        assertEquals("flag_inactive", result.getReason());
    }

    @Test
    @DisplayName("Should return false when no rollout config")
    void testNoRolloutConfig() {
        FeatureFlag flag = FeatureFlag.builder()
                .flagKey("test-flag")
                .name("Test Flag")
                .status("active")
                .createdBy("test")
                .build();

        EvaluationContext context = EvaluationContext.builder()
                .userId("user-123")
                .build();

        EvaluationResult result = evaluator.evaluate(flag, context);

        assertFalse(result.isEnabled());
        assertEquals("no_rollout_config", result.getReason());
    }

    @Test
    @DisplayName("Boolean rollout - enabled for all")
    void testBooleanRolloutEnabled() {
        FeatureFlag flag = FeatureFlag.builder()
                .flagKey("test-flag")
                .name("Test Flag")
                .status("active")
                .createdBy("test")
                .rollout(Map.of("type", "boolean", "value", true))
                .build();

        EvaluationContext context = EvaluationContext.builder()
                .userId("user-123")
                .build();

        EvaluationResult result = evaluator.evaluate(flag, context);

        assertTrue(result.isEnabled());
        assertEquals("rollout:boolean", result.getReason());
    }

    @Test
    @DisplayName("Boolean rollout - disabled for all")
    void testBooleanRolloutDisabled() {
        FeatureFlag flag = FeatureFlag.builder()
                .flagKey("test-flag")
                .name("Test Flag")
                .status("active")
                .createdBy("test")
                .rollout(Map.of("type", "boolean", "value", false))
                .build();

        EvaluationContext context = EvaluationContext.builder()
                .userId("user-123")
                .build();

        EvaluationResult result = evaluator.evaluate(flag, context);

        assertFalse(result.isEnabled());
        assertEquals("rollout:boolean", result.getReason());
    }

    @Test
    @DisplayName("Whitelist rollout - exact match")
    void testWhitelistExactMatch() {
        FeatureFlag flag = FeatureFlag.builder()
                .flagKey("test-flag")
                .name("Test Flag")
                .status("active")
                .createdBy("test")
                .rollout(Map.of("type", "whitelist", "userIds", List.of("user-123", "user-456")))
                .build();

        EvaluationContext context = EvaluationContext.builder()
                .userId("user-123")
                .build();

        EvaluationResult result = evaluator.evaluate(flag, context);

        assertTrue(result.isEnabled());
        assertEquals("rollout:whitelist", result.getReason());
    }

    @Test
    @DisplayName("Whitelist rollout - no match")
    void testWhitelistNoMatch() {
        FeatureFlag flag = FeatureFlag.builder()
                .flagKey("test-flag")
                .name("Test Flag")
                .status("active")
                .createdBy("test")
                .rollout(Map.of("type", "whitelist", "userIds", List.of("user-123", "user-456")))
                .build();

        EvaluationContext context = EvaluationContext.builder()
                .userId("user-789")
                .build();

        EvaluationResult result = evaluator.evaluate(flag, context);

        assertFalse(result.isEnabled());
    }

    @Test
    @DisplayName("Whitelist rollout - wildcard prefix")
    void testWhitelistWildcardPrefix() {
        FeatureFlag flag = FeatureFlag.builder()
                .flagKey("test-flag")
                .name("Test Flag")
                .status("active")
                .createdBy("test")
                .rollout(Map.of("type", "whitelist", "userIds", List.of("admin-*")))
                .build();

        EvaluationContext context = EvaluationContext.builder()
                .userId("admin-john")
                .build();

        EvaluationResult result = evaluator.evaluate(flag, context);

        assertTrue(result.isEnabled());
    }

    @Test
    @DisplayName("Whitelist rollout - wildcard suffix")
    void testWhitelistWildcardSuffix() {
        FeatureFlag flag = FeatureFlag.builder()
                .flagKey("test-flag")
                .name("Test Flag")
                .status("active")
                .createdBy("test")
                .rollout(Map.of("type", "whitelist", "userIds", List.of("*@example.com")))
                .build();

        EvaluationContext context = EvaluationContext.builder()
                .userId("user@example.com")
                .build();

        EvaluationResult result = evaluator.evaluate(flag, context);

        assertTrue(result.isEnabled());
    }

    @Test
    @DisplayName("Whitelist rollout - wildcard contains")
    void testWhitelistWildcardContains() {
        FeatureFlag flag = FeatureFlag.builder()
                .flagKey("test-flag")
                .name("Test Flag")
                .status("active")
                .createdBy("test")
                .rollout(Map.of("type", "whitelist", "userIds", List.of("*test*")))
                .build();

        EvaluationContext context = EvaluationContext.builder()
                .userId("my-test-user")
                .build();

        EvaluationResult result = evaluator.evaluate(flag, context);

        assertTrue(result.isEnabled());
    }

    @Test
    @DisplayName("Percentage rollout - user in rollout")
    void testPercentageRolloutEnabled() {
        FeatureFlag flag = FeatureFlag.builder()
                .flagKey("test-flag")
                .name("Test Flag")
                .status("active")
                .createdBy("test")
                .rollout(Map.of("type", "percentage", "percentage", 100))
                .build();

        EvaluationContext context = EvaluationContext.builder()
                .userId("user-123")
                .build();

        EvaluationResult result = evaluator.evaluate(flag, context);

        assertTrue(result.isEnabled());
        assertEquals("rollout:percentage", result.getReason());
    }

    @Test
    @DisplayName("Percentage rollout - 0% should disable all")
    void testPercentageRolloutZero() {
        FeatureFlag flag = FeatureFlag.builder()
                .flagKey("test-flag")
                .name("Test Flag")
                .status("active")
                .createdBy("test")
                .rollout(Map.of("type", "percentage", "percentage", 0))
                .build();

        EvaluationContext context = EvaluationContext.builder()
                .userId("user-123")
                .build();

        EvaluationResult result = evaluator.evaluate(flag, context);

        assertFalse(result.isEnabled());
    }

    @Test
    @DisplayName("Percentage rollout - no userId should return false")
    void testPercentageRolloutNoUserId() {
        FeatureFlag flag = FeatureFlag.builder()
                .flagKey("test-flag")
                .name("Test Flag")
                .status("active")
                .createdBy("test")
                .rollout(Map.of("type", "percentage", "percentage", 50))
                .build();

        EvaluationContext context = EvaluationContext.builder().build();

        EvaluationResult result = evaluator.evaluate(flag, context);

        assertFalse(result.isEnabled());
    }

    @Test
    @DisplayName("Region rollout - matching region")
    void testRegionRolloutMatch() {
        FeatureFlag flag = FeatureFlag.builder()
                .flagKey("test-flag")
                .name("Test Flag")
                .status("active")
                .createdBy("test")
                .rollout(Map.of("type", "region", "regions", List.of("us-west", "us-east")))
                .build();

        EvaluationContext context = EvaluationContext.builder()
                .userId("user-123")
                .region("us-west")
                .build();

        EvaluationResult result = evaluator.evaluate(flag, context);

        assertTrue(result.isEnabled());
        assertEquals("rollout:region", result.getReason());
    }

    @Test
    @DisplayName("Region rollout - non-matching region")
    void testRegionRolloutNoMatch() {
        FeatureFlag flag = FeatureFlag.builder()
                .flagKey("test-flag")
                .name("Test Flag")
                .status("active")
                .createdBy("test")
                .rollout(Map.of("type", "region", "regions", List.of("us-west", "us-east")))
                .build();

        EvaluationContext context = EvaluationContext.builder()
                .userId("user-123")
                .region("eu-central")
                .build();

        EvaluationResult result = evaluator.evaluate(flag, context);

        assertFalse(result.isEnabled());
    }

    @Test
    @DisplayName("Region rollout - no region in context")
    void testRegionRolloutNoRegion() {
        FeatureFlag flag = FeatureFlag.builder()
                .flagKey("test-flag")
                .name("Test Flag")
                .status("active")
                .createdBy("test")
                .rollout(Map.of("type", "region", "regions", List.of("us-west")))
                .build();

        EvaluationContext context = EvaluationContext.builder()
                .userId("user-123")
                .build();

        EvaluationResult result = evaluator.evaluate(flag, context);

        assertFalse(result.isEnabled());
    }

    @Test
    @DisplayName("Unknown rollout type should default to disabled")
    void testUnknownRolloutType() {
        FeatureFlag flag = FeatureFlag.builder()
                .flagKey("test-flag")
                .name("Test Flag")
                .status("active")
                .createdBy("test")
                .rollout(Map.of("type", "unknown-type"))
                .build();

        EvaluationContext context = EvaluationContext.builder()
                .userId("user-123")
                .build();

        EvaluationResult result = evaluator.evaluate(flag, context);

        assertFalse(result.isEnabled());
        assertEquals("rollout:unknown-type", result.getReason());
    }

    @Test
    @DisplayName("Evaluation result should include context data")
    void testEvaluationResultIncludesContext() {
        FeatureFlag flag = FeatureFlag.builder()
                .flagKey("test-flag")
                .name("Test Flag")
                .status("active")
                .createdBy("test")
                .releaseVersion("v1.2.3")
                .rollout(Map.of("type", "boolean", "value", true))
                .build();

        EvaluationContext context = EvaluationContext.builder()
                .userId("user-123")
                .region("us-west")
                .build();

        EvaluationResult result = evaluator.evaluate(flag, context);

        assertEquals("test-flag", result.getFlagKey());
        assertEquals("user-123", result.getUserId());
        assertEquals("us-west", result.getRegion());
        assertEquals("v1.2.3", result.getReleaseVersion());
    }
}
