package com.aligntech.evaluation.service;

import com.aligntech.domain.FeatureFlag;
import com.aligntech.evaluation.dto.ExplainRequest;
import com.aligntech.evaluation.dto.ExplainResponse;
import com.aligntech.repository.FeatureFlagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExplainabilityServiceTest {

    @Mock
    private FeatureFlagRepository flagRepo;

    @InjectMocks
    private ExplainabilityService explainabilityService;

    private FeatureFlag activeFlag;
    private ExplainRequest request;

    @BeforeEach
    void setUp() {
        activeFlag = FeatureFlag.builder()
                .flagKey("test-flag")
                .name("Test Flag")
                .status("active")
                .flagType("release")
                .createdBy("test-team")
                .releaseVersion("v1.0.0")
                .environments(List.of("production"))
                .activatedAt(Instant.parse("2026-05-01T10:00:00Z"))
                .build();

        request = ExplainRequest.builder()
                .userId("user-123")
                .region("us-west")
                .platform("web")
                .appVersion("2.1.0")
                .build();
    }

    @Test
    @DisplayName("Should throw exception when flag not found")
    void testFlagNotFound() {
        when(flagRepo.findByFlagKey("missing-flag")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                explainabilityService.explain("missing-flag", request)
        );
    }

    @Test
    @DisplayName("Should explain inactive flag")
    void testExplainInactiveFlag() {
        activeFlag.setStatus("draft");
        when(flagRepo.findByFlagKey("test-flag")).thenReturn(Optional.of(activeFlag));

        ExplainResponse response = explainabilityService.explain("test-flag", request);

        assertFalse(response.isEnabled());
        assertTrue(response.getReason().contains("not active"));
        assertEquals("N/A", response.getRolloutExplanation().getRolloutType());
    }

    @Test
    @DisplayName("Should explain boolean rollout - enabled")
    void testExplainBooleanEnabled() {
        activeFlag.setRollout(Map.of("type", "boolean", "value", true));
        when(flagRepo.findByFlagKey("test-flag")).thenReturn(Optional.of(activeFlag));

        ExplainResponse response = explainabilityService.explain("test-flag", request);

        assertTrue(response.isEnabled());
        assertEquals("boolean", response.getRolloutExplanation().getRolloutType());
        assertTrue(response.getRolloutExplanation().getMatchReason().contains("enabled for all users"));
    }

    @Test
    @DisplayName("Should explain boolean rollout - disabled")
    void testExplainBooleanDisabled() {
        activeFlag.setRollout(Map.of("type", "boolean", "value", false));
        when(flagRepo.findByFlagKey("test-flag")).thenReturn(Optional.of(activeFlag));

        ExplainResponse response = explainabilityService.explain("test-flag", request);

        assertFalse(response.isEnabled());
        assertEquals("boolean", response.getRolloutExplanation().getRolloutType());
        assertTrue(response.getRolloutExplanation().getMatchReason().contains("disabled for all users"));
    }

    @Test
    @DisplayName("Should explain whitelist rollout - match")
    void testExplainWhitelistMatch() {
        activeFlag.setRollout(Map.of(
                "type", "whitelist",
                "userIds", List.of("user-123", "user-456")
        ));
        when(flagRepo.findByFlagKey("test-flag")).thenReturn(Optional.of(activeFlag));

        ExplainResponse response = explainabilityService.explain("test-flag", request);

        assertTrue(response.isEnabled());
        assertEquals("whitelist", response.getRolloutExplanation().getRolloutType());
        assertTrue(response.getRolloutExplanation().getMatchReason().contains("matches whitelist pattern"));
        assertTrue(response.getRolloutExplanation().getMatchReason().contains("user-123"));
    }

    @Test
    @DisplayName("Should explain whitelist rollout - no match")
    void testExplainWhitelistNoMatch() {
        activeFlag.setRollout(Map.of(
                "type", "whitelist",
                "userIds", List.of("admin-*", "test-*")
        ));
        when(flagRepo.findByFlagKey("test-flag")).thenReturn(Optional.of(activeFlag));

        ExplainResponse response = explainabilityService.explain("test-flag", request);

        assertFalse(response.isEnabled());
        assertTrue(response.getRolloutExplanation().getMatchReason().contains("does not match"));
    }

    @Test
    @DisplayName("Should explain percentage rollout")
    void testExplainPercentageRollout() {
        activeFlag.setRollout(Map.of("type", "percentage", "percentage", 50));
        when(flagRepo.findByFlagKey("test-flag")).thenReturn(Optional.of(activeFlag));

        ExplainResponse response = explainabilityService.explain("test-flag", request);

        assertEquals("percentage", response.getRolloutExplanation().getRolloutType());
        assertTrue(response.getRolloutExplanation().getMatchReason().contains("hash"));
        assertTrue(response.getRolloutExplanation().getMatchReason().contains("50%"));
    }

    @Test
    @DisplayName("Should explain region rollout - match")
    void testExplainRegionMatch() {
        activeFlag.setRollout(Map.of(
                "type", "region",
                "regions", List.of("us-west", "us-east")
        ));
        when(flagRepo.findByFlagKey("test-flag")).thenReturn(Optional.of(activeFlag));

        ExplainResponse response = explainabilityService.explain("test-flag", request);

        assertTrue(response.isEnabled());
        assertEquals("region", response.getRolloutExplanation().getRolloutType());
        assertTrue(response.getRolloutExplanation().getMatchReason().contains("us-west"));
        assertTrue(response.getRolloutExplanation().getMatchReason().contains("is in allowed regions"));
    }

    @Test
    @DisplayName("Should explain region rollout - no match")
    void testExplainRegionNoMatch() {
        activeFlag.setRollout(Map.of(
                "type", "region",
                "regions", List.of("eu-central", "ap-south")
        ));
        when(flagRepo.findByFlagKey("test-flag")).thenReturn(Optional.of(activeFlag));

        ExplainResponse response = explainabilityService.explain("test-flag", request);

        assertFalse(response.isEnabled());
        assertTrue(response.getRolloutExplanation().getMatchReason().contains("is not in allowed regions"));
    }

    @Test
    @DisplayName("Should include flag context in response")
    void testIncludesFlagContext() {
        activeFlag.setRollout(Map.of("type", "boolean", "value", true));
        when(flagRepo.findByFlagKey("test-flag")).thenReturn(Optional.of(activeFlag));

        ExplainResponse response = explainabilityService.explain("test-flag", request);

        assertNotNull(response.getFlagContext());
        assertEquals("active", response.getFlagContext().getStatus());
        assertEquals("release", response.getFlagContext().getFlagType());
        assertEquals("v1.0.0", response.getFlagContext().getReleaseVersion());
        assertEquals("test-team", response.getFlagContext().getCreatedBy());
        assertNotNull(response.getFlagContext().getActivatedAt());
    }

    @Test
    @DisplayName("Should include user context in response")
    void testIncludesUserContext() {
        activeFlag.setRollout(Map.of("type", "boolean", "value", true));
        when(flagRepo.findByFlagKey("test-flag")).thenReturn(Optional.of(activeFlag));

        ExplainResponse response = explainabilityService.explain("test-flag", request);

        assertNotNull(response.getUserContext());
        assertEquals("user-123", response.getUserContext().getUserId());
        assertEquals("us-west", response.getUserContext().getRegion());
        assertEquals("web", response.getUserContext().getPlatform());
        assertEquals("2.1.0", response.getUserContext().getAppVersion());
    }

    @Test
    @DisplayName("Should include applicable rules")
    void testIncludesApplicableRules() {
        activeFlag.setRollout(Map.of(
                "type", "whitelist",
                "userIds", List.of("user-123")
        ));
        when(flagRepo.findByFlagKey("test-flag")).thenReturn(Optional.of(activeFlag));

        ExplainResponse response = explainabilityService.explain("test-flag", request);

        assertNotNull(response.getApplicableRules());
        assertFalse(response.getApplicableRules().isEmpty());
        assertTrue(response.getApplicableRules().stream()
                .anyMatch(rule -> rule.contains("active")));
    }

    @Test
    @DisplayName("Should handle missing rollout config")
    void testMissingRolloutConfig() {
        activeFlag.setRollout(null);
        when(flagRepo.findByFlagKey("test-flag")).thenReturn(Optional.of(activeFlag));

        ExplainResponse response = explainabilityService.explain("test-flag", request);

        assertFalse(response.isEnabled());
        assertTrue(response.getReason().contains("No rollout configuration"));
    }

    @Test
    @DisplayName("Should set evaluation timestamp")
    void testEvaluationTimestamp() {
        activeFlag.setRollout(Map.of("type", "boolean", "value", true));
        when(flagRepo.findByFlagKey("test-flag")).thenReturn(Optional.of(activeFlag));

        Instant before = Instant.now();
        ExplainResponse response = explainabilityService.explain("test-flag", request);
        Instant after = Instant.now();

        assertNotNull(response.getEvaluatedAt());
        assertTrue(response.getEvaluatedAt().isAfter(before.minusSeconds(1)));
        assertTrue(response.getEvaluatedAt().isBefore(after.plusSeconds(1)));
    }
}
