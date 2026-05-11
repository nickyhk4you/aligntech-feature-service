package com.aligntech.management.service;

import com.aligntech.management.dto.CreateFlagRequest;
import com.aligntech.domain.FeatureFlag;
import com.aligntech.event.FlagChangeEvent;
import com.aligntech.event.FlagChangePublisher;
import com.aligntech.repository.FeatureFlagRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FlagManagementService {

    private final FeatureFlagRepository flagRepo;
    private final FlagChangePublisher eventPublisher;

    public FlagManagementService(FeatureFlagRepository flagRepo,
                                  @Autowired(required = false) FlagChangePublisher eventPublisher) {
        this.flagRepo = flagRepo;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public FeatureFlag createFlag(CreateFlagRequest request) {
        if (flagRepo.findByFlagKey(request.getFlagKey()).isPresent()) {
            throw new IllegalArgumentException("Flag with key '" + request.getFlagKey() + "' already exists");
        }

        FeatureFlag flag = FeatureFlag.builder()
                .flagKey(request.getFlagKey())
                .name(request.getName())
                .description(request.getDescription())
                .flagType(request.getFlagType())
                .status(request.getStatus())
                .environments(request.getEnvironments())
                .createdBy(request.getCreatedBy())
                .releaseVersion(request.getReleaseVersion())
                .rollout(request.getRollout())
                .build();

        FeatureFlag saved = flagRepo.save(flag);

        if (eventPublisher != null) eventPublisher.publish(FlagChangeEvent.builder()
                .flagId(saved.getId())
                .flagKey(saved.getFlagKey())
                .action("created")
                .timestamp(Instant.now())
                .changedBy(request.getCreatedBy())
                .build());

        log.info("Created flag: key={}", saved.getFlagKey());
        return saved;
    }

    @Transactional
    public FeatureFlag activateFlag(UUID id, String activatedBy) {
        FeatureFlag flag = flagRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Flag not found: " + id));

        if ("active".equals(flag.getStatus())) {
            throw new IllegalStateException("Flag is already active");
        }

        flag.setStatus("active");
        flag.setActivatedAt(Instant.now());
        FeatureFlag saved = flagRepo.save(flag);

        if (eventPublisher != null) eventPublisher.publish(FlagChangeEvent.builder()
                .flagId(saved.getId())
                .flagKey(saved.getFlagKey())
                .action("activated")
                .timestamp(Instant.now())
                .changedBy(activatedBy)
                .build());

        return saved;
    }

    public FeatureFlag getFlag(UUID id) {
        return flagRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Flag not found: " + id));
    }

    public List<FeatureFlag> listFlags(String status, Pageable pageable) {
        if (status != null) return flagRepo.findByStatus(status);
        Page<FeatureFlag> page = flagRepo.findAll(pageable);
        return page.getContent();
    }

    @Transactional
    public void archiveFlag(UUID id, String archivedBy) {
        FeatureFlag flag = flagRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Flag not found: " + id));

        flag.setStatus("archived");
        flag.setArchivedAt(Instant.now());
        flagRepo.save(flag);

        if (eventPublisher != null) eventPublisher.publish(FlagChangeEvent.builder()
                .flagId(flag.getId())
                .flagKey(flag.getFlagKey())
                .action("archived")
                .timestamp(Instant.now())
                .changedBy(archivedBy)
                .build());

        log.info("Archived flag: key={}", flag.getFlagKey());
    }
}
