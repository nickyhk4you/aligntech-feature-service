package com.aligntech.evaluation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplainRequest {

    @NotBlank
    private String userId;
    
    private String tenantId;
    private String region;
    private String appVersion;
    private String platform;
    
    @Builder.Default
    private Map<String, Object> customAttributes = new HashMap<>();
}
