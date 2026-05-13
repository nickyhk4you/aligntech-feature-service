package com.aligntech.integration;

import com.aligntech.domain.FeatureFlag;
import com.aligntech.evaluation.dto.EvaluationRequest;
import com.aligntech.evaluation.dto.EvaluationResponse;
import com.aligntech.evaluation.engine.EvaluationContext;
import com.aligntech.repository.FeatureFlagRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class EvaluationApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.profiles.active", () -> "test");
        registry.add("spring.cache.type", () -> "none");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FeatureFlagRepository flagRepository;

    @BeforeEach
    void setUp() {
        flagRepository.deleteAll();
    }

    @Test
    void testEvaluateActiveFlag() throws Exception {
        FeatureFlag flag = FeatureFlag.builder()
                .flagKey("test-feature")
                .name("Test Feature")
                .status("active")
                .createdBy("test")
                .rollout(Map.of("type", "boolean", "value", true))
                .build();
        flagRepository.save(flag);

        EvaluationRequest request = new EvaluationRequest();
        request.setContext(EvaluationContext.builder()
                .userId("user-123")
                .region("us-west")
                .build());
        request.setFlagKeys(List.of("test-feature"));

        mockMvc.perform(post("/api/v1/evaluate")
                        .header("X-App-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", hasSize(1)))
                .andExpect(jsonPath("$.results[0].flagKey", is("test-feature")))
                .andExpect(jsonPath("$.results[0].enabled", is(true)));
    }

    @Test
    void testEvaluateInactiveFlag() throws Exception {
        FeatureFlag flag = FeatureFlag.builder()
                .flagKey("test-feature")
                .name("Test Feature")
                .status("draft")
                .createdBy("test")
                .rollout(Map.of("type", "boolean", "value", true))
                .build();
        flagRepository.save(flag);

        EvaluationRequest request = new EvaluationRequest();
        request.setContext(EvaluationContext.builder()
                .userId("user-123")
                .build());
        request.setFlagKeys(List.of("test-feature"));

        mockMvc.perform(post("/api/v1/evaluate")
                        .header("X-App-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", hasSize(0)));
    }

    @Test
    void testSnapshot() throws Exception {
        FeatureFlag flag1 = FeatureFlag.builder()
                .flagKey("feature-1")
                .name("Feature 1")
                .status("active")
                .createdBy("test")
                .rollout(Map.of("type", "boolean", "value", true))
                .build();

        FeatureFlag flag2 = FeatureFlag.builder()
                .flagKey("feature-2")
                .name("Feature 2")
                .status("active")
                .createdBy("test")
                .rollout(Map.of("type", "boolean", "value", false))
                .build();

        flagRepository.save(flag1);
        flagRepository.save(flag2);

        mockMvc.perform(get("/api/v1/snapshot")
                        .header("X-App-Key", "test-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version", notNullValue()))
                .andExpect(jsonPath("$.flags", hasSize(2)))
                .andExpect(jsonPath("$.flags[*].flagKey", containsInAnyOrder("feature-1", "feature-2")));
    }

    @Test
    void testExplain() throws Exception {
        FeatureFlag flag = FeatureFlag.builder()
                .flagKey("premium-feature")
                .name("Premium Feature")
                .status("active")
                .flagType("release")
                .releaseVersion("v2.0.0")
                .createdBy("product-team")
                .rollout(Map.of(
                        "type", "region",
                        "regions", List.of("us-west", "us-east")
                ))
                .build();
        flagRepository.save(flag);

        mockMvc.perform(get("/api/v1/explain/premium-feature")
                        .header("X-App-Key", "test-key")
                        .param("userId", "user-123")
                        .param("region", "us-west"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flagKey", is("premium-feature")))
                .andExpect(jsonPath("$.enabled", is(true)))
                .andExpect(jsonPath("$.rolloutExplanation.rolloutType", is("region")))
                .andExpect(jsonPath("$.rolloutExplanation.matched", is(true)))
                .andExpect(jsonPath("$.flagContext.releaseVersion", is("v2.0.0")));
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    void testEvaluateWithWhitelistRollout() throws Exception {
        FeatureFlag flag = FeatureFlag.builder()
                .flagKey("admin-panel")
                .name("Admin Panel")
                .status("active")
                .createdBy("test")
                .rollout(Map.of(
                        "type", "whitelist",
                        "userIds", List.of("admin-*", "user-123")
                ))
                .build();
        flagRepository.save(flag);

        EvaluationRequest request = new EvaluationRequest();
        request.setContext(EvaluationContext.builder()
                .userId("admin-john")
                .build());
        request.setFlagKeys(List.of("admin-panel"));

        mockMvc.perform(post("/api/v1/evaluate")
                        .header("X-App-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].enabled", is(true)));
    }

    @Test
    void testEvaluateWithPercentageRollout() throws Exception {
        FeatureFlag flag = FeatureFlag.builder()
                .flagKey("beta-feature")
                .name("Beta Feature")
                .status("active")
                .createdBy("test")
                .rollout(Map.of(
                        "type", "percentage",
                        "percentage", 100
                ))
                .build();
        flagRepository.save(flag);

        EvaluationRequest request = new EvaluationRequest();
        request.setContext(EvaluationContext.builder()
                .userId("user-123")
                .build());
        request.setFlagKeys(List.of("beta-feature"));

        mockMvc.perform(post("/api/v1/evaluate")
                        .header("X-App-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].enabled", is(true)));
    }
}
