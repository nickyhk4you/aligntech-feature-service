package com.aligntech.evaluation.service;

import com.aligntech.evaluation.dto.SnapshotResponse;
import com.aligntech.domain.FeatureFlag;
import com.aligntech.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SnapshotService {

    private final FeatureFlagRepository flagRepo;

    @Cacheable("flagSnapshot")
    public SnapshotResponse buildSnapshot() {
        List<FeatureFlag> flags = flagRepo.findAllActive();

        List<SnapshotResponse.FlagSnapshot> flagSnapshots = flags.stream()
                .map(f -> SnapshotResponse.FlagSnapshot.builder()
                        .flagKey(f.getFlagKey())
                        .flagType(f.getFlagType())
                        .status(f.getStatus())
                        .defaultValue(false)
                        .rollout(f.getRollout())
                        .build())
                .toList();

        return SnapshotResponse.builder()
                .version(String.valueOf(Instant.now().toEpochMilli()))
                .generatedAt(Instant.now())
                .flags(flagSnapshots)
                .build();
    }
}
