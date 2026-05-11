package com.aligntech.management.dto;

import com.aligntech.domain.FeatureFlag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlagResponse {

    private UUID id;
    private String flagKey;
    private String name;
    private String description;
    private String flagType;
    private String status;
    private List<String> environments;
    private String createdBy;
    private String releaseVersion;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant activatedAt;
    private Instant archivedAt;
    private Map<String, Object> rollout;

    public static FlagResponse from(FeatureFlag flag) {
        return FlagResponse.builder()
                .id(flag.getId())
                .flagKey(flag.getFlagKey())
                .name(flag.getName())
                .description(flag.getDescription())
                .flagType(flag.getFlagType())
                .status(flag.getStatus())
                .environments(flag.getEnvironments())
                .createdBy(flag.getCreatedBy())
                .releaseVersion(flag.getReleaseVersion())
                .createdAt(flag.getCreatedAt())
                .updatedAt(flag.getUpdatedAt())
                .activatedAt(flag.getActivatedAt())
                .archivedAt(flag.getArchivedAt())
                .rollout(flag.getRollout())
                .build();
    }
}
