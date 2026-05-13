package com.aligntech.management.controller;

import com.aligntech.management.dto.*;
import com.aligntech.management.service.AuditService;
import com.aligntech.management.service.FlagManagementService;
import com.aligntech.domain.FeatureFlag;
import com.aligntech.security.PermissionService;
import com.aligntech.security.SecurityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class FeatureFlagController {

    private final FlagManagementService flagService;
    private final AuditService auditService;
    private final PermissionService permissionService;
    private final SecurityService securityService;

    @PostMapping("/flags")
    public ResponseEntity<FlagResponse> createFlag(@Valid @RequestBody CreateFlagRequest request) {
        if (!permissionService.canCreateFlag()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        FeatureFlag flag = flagService.createFlag(request);
        auditService.logChange(flag.getId(), "created", securityService.getCurrentUsername(),
                "Flag created: " + flag.getName(), null,
                Map.of("flagKey", flag.getFlagKey(), "name", flag.getName(), "flagType", flag.getFlagType()));
        log.debug("flag created: {}", flag.getFlagKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(FlagResponse.from(flag));
    }

    @GetMapping("/flags")
    public List<FlagResponse> listFlags(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return flagService.listFlags(status, PageRequest.of(page, size))
                .stream().map(FlagResponse::from).toList();
    }

    @GetMapping("/flags/{id}")
    public ResponseEntity<FlagResponse> getFlag(@PathVariable UUID id) {
        if (!permissionService.canReadFlag(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(FlagResponse.from(flagService.getFlag(id)));
    }

    @PostMapping("/flags/{id}/activate")
    public ResponseEntity<FlagResponse> activateFlag(@PathVariable UUID id) {
        if (!permissionService.canActivateFlag(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        String activatedBy = securityService.getCurrentUsername();
        FeatureFlag flag = flagService.activateFlag(id, activatedBy);
        auditService.logChange(id, "activated", activatedBy, "Flag activated", null, Map.of("status", "active"));
        return ResponseEntity.ok(FlagResponse.from(flag));
    }

    @DeleteMapping("/flags/{id}")
    public ResponseEntity<Void> archiveFlag(@PathVariable UUID id) {
        if (!permissionService.canDeleteFlag(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        String archivedBy = securityService.getCurrentUsername();
        flagService.archiveFlag(id, archivedBy);
        auditService.logChange(id, "archived", archivedBy, "Flag archived", null, Map.of("status", "archived"));
        return ResponseEntity.noContent().build();
    }
}
