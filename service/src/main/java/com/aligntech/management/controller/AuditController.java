package com.aligntech.management.controller;

import com.aligntech.domain.AuditLog;
import com.aligntech.management.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/flags/{flagId}")
    public List<AuditLog> getFlagAuditLogs(@PathVariable UUID flagId) {
        return auditService.getFlagHistory(flagId);
    }
}
