package com.aligntech.security;

import com.aligntech.domain.FeatureFlag;
import com.aligntech.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final FeatureFlagRepository flagRepository;

    public boolean canReadFlag(UUID flagId) {
        return hasPermission(flagId, Permission.READ);
    }

    public boolean canUpdateFlag(UUID flagId) {
        return hasPermission(flagId, Permission.UPDATE);
    }

    public boolean canDeleteFlag(UUID flagId) {
        return hasPermission(flagId, Permission.DELETE);
    }

    public boolean canActivateFlag(UUID flagId) {
        return hasPermission(flagId, Permission.ACTIVATE);
    }

    public boolean canCreateFlag() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        Set<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return authorities.contains("ROLE_ADMIN") || 
               authorities.contains("SCOPE_admin:flags") ||
               authorities.contains("SCOPE_write:flags");
    }

    private boolean hasPermission(UUID flagId, Permission permission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        String username = auth.getName();
        Set<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // Admin has all permissions
        if (authorities.contains("ROLE_ADMIN") || authorities.contains("SCOPE_admin:flags")) {
            return true;
        }

        // Check per-flag permissions
        Optional<FeatureFlag> flagOpt = flagRepository.findById(flagId);
        if (flagOpt.isEmpty()) {
            return false;
        }

        FeatureFlag flag = flagOpt.get();

        // Owner team members have full access
        if (flag.getOwnerTeam() != null && authorities.contains("TEAM_" + flag.getOwnerTeam())) {
            return true;
        }

        // Creator has full access
        if (flag.getCreatedBy().equals(username)) {
            return true;
        }

        // Check specific permission scopes
        switch (permission) {
            case READ:
                return authorities.contains("SCOPE_read:flags");
            case UPDATE:
            case ACTIVATE:
                return authorities.contains("SCOPE_write:flags");
            case DELETE:
                return authorities.contains("SCOPE_delete:flags");
            default:
                return false;
        }
    }

    public enum Permission {
        READ,
        UPDATE,
        DELETE,
        ACTIVATE
    }
}
