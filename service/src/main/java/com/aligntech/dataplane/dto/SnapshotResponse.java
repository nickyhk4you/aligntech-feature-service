package com.aligntech.dataplane.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class SnapshotResponse {

    private String version;
    private Instant generatedAt;
    private List<FlagSnapshot> flags;

    @Data
    @Builder
    @AllArgsConstructor
    public static class FlagSnapshot {
        private String flagKey;
        private String flagType;
        private String status;
        private boolean defaultValue;
        private Map<String, Object> rollout;
    }
}
