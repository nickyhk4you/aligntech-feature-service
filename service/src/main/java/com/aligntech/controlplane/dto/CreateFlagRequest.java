package com.aligntech.controlplane.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFlagRequest {

    @NotBlank
    @Size(max = 255)
    private String flagKey;

    @NotBlank
    @Size(max = 500)
    private String name;

    @Size(max = 2000)
    private String description;

    @Builder.Default
    private String flagType = "boolean";

    @Builder.Default
    private List<String> environments = List.of("development", "staging", "production");

    @NotBlank
    private String createdBy;

    private String releaseVersion;

    // optional rollout config
    private Map<String, Object> rollout;
}
