package com.cloudFileStorageSystem.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AuthenticationUtil {

    public Long getCurrentUserId() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("User is not authenticated.");
        }

        if (!(authentication.getPrincipal() instanceof CustomUserPrincipal principal)) {
            throw new RuntimeException("Invalid authentication principal.");
        }

        return principal.getId();
    }

    public CustomUserPrincipal getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (!(Objects.requireNonNull(authentication).getPrincipal() instanceof CustomUserPrincipal principal)) {
            throw new RuntimeException("Invalid authentication principal.");
        }

        return principal;
    }
}
