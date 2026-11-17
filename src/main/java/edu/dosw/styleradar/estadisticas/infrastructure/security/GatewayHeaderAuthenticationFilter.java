package edu.dosw.styleradar.estadisticas.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final List<String> SUPPORTED_ROLES =
            List.of("STORE_ADMIN", "FOUNDATION_ADMIN", "PLATFORM_ADMIN");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String role = normalizeRole(request.getHeader("X-User-Role"));
        if (role != null && isCompleteActor(role, request)) {
            AuthenticatedActor actor = new AuthenticatedActor(
                    role,
                    trimToNull(request.getHeader("X-Store-Id")),
                    trimToNull(request.getHeader("X-Foundation-Id")));
            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    actor,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private boolean isCompleteActor(String role, HttpServletRequest request) {
        return switch (role) {
            case "STORE_ADMIN" -> trimToNull(request.getHeader("X-Store-Id")) != null;
            case "FOUNDATION_ADMIN" -> trimToNull(request.getHeader("X-Foundation-Id")) != null;
            case "PLATFORM_ADMIN" -> true;
            default -> false;
        };
    }

    private String normalizeRole(String role) {
        String normalizedRole = trimToNull(role);
        if (normalizedRole == null) {
            return null;
        }
        if (normalizedRole.startsWith("ROLE_")) {
            normalizedRole = normalizedRole.substring("ROLE_".length());
        }
        normalizedRole = normalizedRole.toUpperCase(Locale.ROOT);
        return SUPPORTED_ROLES.contains(normalizedRole) ? normalizedRole : null;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
