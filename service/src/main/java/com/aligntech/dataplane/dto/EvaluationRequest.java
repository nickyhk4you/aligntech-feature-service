package com.aligntech.dataplane.dto;

import com.aligntech.dataplane.engine.EvaluationContext;
import jakarta.validation.constraints.NotNull;
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
public class EvaluationRequest {

    @NotNull
    private EvaluationContext context;

    private List<String> flagKeys;

    @Builder.Default
    private boolean includePayload = true;
}
