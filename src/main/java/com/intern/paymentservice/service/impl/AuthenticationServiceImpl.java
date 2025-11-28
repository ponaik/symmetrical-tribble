package com.intern.paymentservice.service.impl;

import com.intern.paymentservice.service.AuthenticationService;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

@Service
@NoArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Override
    public long getInternalId() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("Authentication is missing");
        }

        Jwt principal = (Jwt) authentication.getPrincipal();
        if (principal == null) {
            throw new IllegalStateException("Authentication principal is missing");
        }

        Map<String, Object> claims = principal.getClaims();
        Long internalId = (Long) claims.get("internal_id");
        if (internalId == null) {
            throw new IllegalStateException("Internal id is missing");
        }
        return internalId;
    }

    @Override
    public boolean isAdmin() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> Objects.equals(authority.getAuthority(), ADMIN));
    }

    @Override
    public boolean isUser() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> Objects.equals(authority.getAuthority(), USER));
    }
}
