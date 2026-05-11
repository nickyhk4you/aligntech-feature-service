package com.aligntech.evaluation.controller;

import com.aligntech.evaluation.dto.EvaluationRequest;
import com.aligntech.evaluation.dto.EvaluationResponse;
import com.aligntech.evaluation.dto.SnapshotResponse;
import com.aligntech.evaluation.engine.EvaluationResult;
import com.aligntech.evaluation.service.EvaluationService;
import com.aligntech.evaluation.service.SnapshotService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final SnapshotService snapshotService;
    private final MeterRegistry meterRegistry;

    public EvaluationController(EvaluationService service, SnapshotService snapshot,
                                 MeterRegistry registry) {
        this.evaluationService = service;
        this.snapshotService = snapshot;
        this.meterRegistry = registry;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<EvaluationResponse> evaluate(@Valid @RequestBody EvaluationRequest request) {
        meterRegistry.counter("api.evaluate.calls").increment();
        List<EvaluationResult> results = evaluationService.evaluateFlags(
                request.getContext(), request.getFlagKeys());
        return ResponseEntity.ok(EvaluationResponse.from(results));
    }

    @GetMapping("/snapshot")
    public SnapshotResponse snapshot() {
        meterRegistry.counter("api.snapshot.calls").increment();
        return snapshotService.buildSnapshot();
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
