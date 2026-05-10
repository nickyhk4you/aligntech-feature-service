package com.aligntech.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlagChangeEvent {

    private UUID flagId;
    private String flagKey;
    private String action;
    private Instant timestamp;
    private String changedBy;
}
