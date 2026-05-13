package com.aligntech.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SecurityService {

    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public String getCurrentUsername() {
        Authentication auth = getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    public Set<String> getCurrentUserAuthorities() {
        Authentication auth = getAuthentication();
        if (auth == null) {
            return Set.of();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    public boolean hasRole(String role) {
        return getCurrentUserAuthorities().contains("ROLE_" + role);
    }

    public boolean hasAuthority(String authority) {
        return getCurrentUserAuthorities().contains(authority);
    }

    public boolean isAdmin() {
        return hasRole("ADMIN") || hasAuthority("SCOPE_admin:flags");
    }

    public boolean canReadFlags() {
        return hasAuthority("SCOPE_read:flags") || hasRole("USER") || isAdmin();
    }

    public boolean canWriteFlags() {
        return hasAuthority("SCOPE_write:flags") || isAdmin();
    }

    public boolean canDeleteFlags() {
        return hasAuthority("SCOPE_delete:flags") || isAdmin();
    }
}
