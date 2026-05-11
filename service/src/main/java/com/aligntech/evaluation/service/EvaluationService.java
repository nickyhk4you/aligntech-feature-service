package com.aligntech.evaluation.service;

import com.aligntech.evaluation.engine.EvaluationContext;
import com.aligntech.evaluation.engine.EvaluationResult;
import com.aligntech.evaluation.engine.RuleEvaluator;
import com.aligntech.domain.FeatureFlag;
import com.aligntech.repository.FeatureFlagRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EvaluationService {

    private final FeatureFlagRepository flagRepo;
    private final RuleEvaluator evaluator;
    private final MeterRegistry meterRegistry;
    private final ObservationRegistry observationRegistry;

    public EvaluationService(FeatureFlagRepository flagRepo,
                             RuleEvaluator evaluator,
                             MeterRegistry registry,
                             ObservationRegistry observationRegistry) {
        this.flagRepo = flagRepo;
        this.evaluator = evaluator;
        this.meterRegistry = registry;
        this.observationRegistry = observationRegistry;
    }

    public List<EvaluationResult> evaluateFlags(EvaluationContext context, List<String> flagKeys) {
        return Observation.createNotStarted("evaluation.evaluate-flags", observationRegistry)
                .lowCardinalityKeyValue("context.userId", context.getUserId() != null ? context.getUserId() : "anonymous")
                .observe(() -> {
                    long start = System.nanoTime();

                    List<FeatureFlag> flags;
                    if (flagKeys == null || flagKeys.isEmpty()) {
                        flags = flagRepo.findAllActive();
                    } else {
                        flags = flagKeys.stream()
                                .map(flagRepo::findByFlagKey)
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .filter(FeatureFlag::isActive)
                                .collect(Collectors.toList());
                    }

                    List<EvaluationResult> results = flags.stream()
                            .map(flag -> evaluator.evaluate(flag, context))
                            .toList();

                    long elapsed = System.nanoTime() - start;
                    meterRegistry.timer("evaluation.duration").record(elapsed, TimeUnit.NANOSECONDS);
                    meterRegistry.counter("evaluation.requests").increment();
                    meterRegistry.counter("evaluation.flags", "outcome", "evaluated").increment(results.size());

                    return results;
                });
    }
}
