package com.residential.management.demo.residential.infrastructure.authorization.sfs;

import org.springframework.security.core.context.SecurityContextHolder;

/** Usuario autenticado de la peticion actual (lo llena JwtAuthenticationFilter). */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getDetails() instanceof Long id ? id : null;
    }

    public static boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
