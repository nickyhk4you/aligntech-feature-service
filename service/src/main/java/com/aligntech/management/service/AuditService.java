package com.aligntech.management.service;

import com.aligntech.domain.AuditLog;
import com.aligntech.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepo;

    public void logChange(UUID flagId, String action, String changedBy,
                          String summary, Map<String, Object> oldValue,
                          Map<String, Object> newValue) {
        AuditLog entry = new AuditLog(flagId, action, changedBy, summary, oldValue, newValue);
        auditLogRepo.save(entry);
    }

    public List<AuditLog> getFlagHistory(UUID flagId) {
        return auditLogRepo.findByFlagIdOrderByChangedAtDesc(flagId);
    }
}
