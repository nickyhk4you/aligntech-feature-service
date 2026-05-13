package com.aligntech.evaluation.controller;

import com.aligntech.evaluation.dto.ExplainRequest;
import com.aligntech.evaluation.dto.ExplainResponse;
import com.aligntech.evaluation.service.ExplainabilityService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ExplainController {

    private final ExplainabilityService explainabilityService;
    private final MeterRegistry meterRegistry;

    @PostMapping("/explain/{flagKey}")
    public ExplainResponse explain(
            @PathVariable String flagKey,
            @Valid @RequestBody ExplainRequest request) {
        
        meterRegistry.counter("api.explain.calls", "flagKey", flagKey).increment();
        
        return explainabilityService.explain(flagKey, request);
    }

    @GetMapping("/explain/{flagKey}")
    public ExplainResponse explainGet(
            @PathVariable String flagKey,
            @RequestParam String userId,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String appVersion,
            @RequestParam(required = false) String tenantId) {
        
        meterRegistry.counter("api.explain.calls", "flagKey", flagKey).increment();
        
        ExplainRequest request = ExplainRequest.builder()
                .userId(userId)
                .region(region)
                .platform(platform)
                .appVersion(appVersion)
                .tenantId(tenantId)
                .build();
        
        return explainabilityService.explain(flagKey, request);
    }
}
