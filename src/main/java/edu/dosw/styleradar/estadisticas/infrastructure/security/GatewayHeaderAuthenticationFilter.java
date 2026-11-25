package edu.dosw.styleradar.estadisticas.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtTokenValidator jwtTokenValidator;

    public GatewayHeaderAuthenticationFilter() {
        this(null);
    }

    public GatewayHeaderAuthenticationFilter(JwtTokenValidator jwtTokenValidator) {
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            if (jwtTokenValidator == null) {
                rejectUnauthorized(response, request.getRequestURI(), "JWT validation is not configured");
                return;
            }
            try {
                AuthenticatedActor actor = jwtTokenValidator.validateAndExtract(
                        token,
                        request.getHeader("X-Store-Id"),
                        request.getHeader("X-Foundation-Id"));
                setAuthentication(actor);
                filterChain.doFilter(request, response);
                return;
            } catch (JwtTokenValidator.InvalidJwtException ex) {
                SecurityContextHolder.clearContext();
                rejectUnauthorized(response, request.getRequestURI(), ex.getMessage());
                return;
            }
        }

        String role = JwtTokenValidator.normalizeRoleName(request.getHeader("X-User-Role"));
        if (role != null && isCompleteActor(role, request)) {
            AuthenticatedActor actor = new AuthenticatedActor(
                    role,
                    trimToNull(request.getHeader("X-Store-Id")),
                    trimToNull(request.getHeader("X-Foundation-Id")));
            setAuthentication(actor);
        }
        filterChain.doFilter(request, response);
    }

    private void setAuthentication(AuthenticatedActor actor) {
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                actor,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + actor.role())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void rejectUnauthorized(HttpServletResponse response, String path, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String safeMessage = message != null ? message.replace("\"", "'") : "Unauthorized";
        String safePath = path != null ? path.replace("\"", "'") : "";
        response.getWriter().write(
                "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + safeMessage + "\",\"path\":\"" + safePath + "\"}"
        );
    }

    private boolean isCompleteActor(String role, HttpServletRequest request) {
        return switch (role) {
            case "STORE_ADMIN" -> trimToNull(request.getHeader("X-Store-Id")) != null;
            case "FOUNDATION_ADMIN" -> trimToNull(request.getHeader("X-Foundation-Id")) != null;
            case "PLATFORM_ADMIN", "COMPRADOR" -> true;
            default -> false;
        };
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
