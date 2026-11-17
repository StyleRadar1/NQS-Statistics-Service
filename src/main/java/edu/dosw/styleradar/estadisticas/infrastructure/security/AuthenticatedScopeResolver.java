package edu.dosw.styleradar.estadisticas.infrastructure.security;

import edu.dosw.styleradar.estadisticas.domain.exception.UnauthorizedScopeException;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedScopeResolver {

    private static final String PLATFORM_SCOPE_ID = "platform";

    public Scope resolve(MetricCode metricCode) {
        AuthenticatedActor actor = authenticatedActor();
        ScopeType requestedType = metricCode.scopeType();
        return switch (actor.role()) {
            case "STORE_ADMIN", "ADMIN_ALMACEN" -> {
                if (requestedType != ScopeType.STORE) {
                    throw unauthorized("STORE_ADMIN can only access STORE metrics");
                }
                yield scope(actor.storeId(), ScopeType.STORE);
            }
            case "FOUNDATION_ADMIN", "REPRESENTANTE_FUNDACION" -> {
                if (requestedType != ScopeType.FOUNDATION) {
                    throw unauthorized("FOUNDATION_ADMIN can only access FOUNDATION metrics");
                }
                yield scope(actor.foundationId(), ScopeType.FOUNDATION);
            }
            case "PLATFORM_ADMIN", "ADMIN_STYLERADAR" -> {
                if (requestedType != ScopeType.PLATFORM) {
                    throw unauthorized("PLATFORM_ADMIN can only access PLATFORM metrics");
                }
                yield new Scope(PLATFORM_SCOPE_ID, ScopeType.PLATFORM);
            }
            default -> throw unauthorized("The authenticated role cannot access metric scopes");
        };
    }

    private AuthenticatedActor authenticatedActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedActor actor)) {
            throw unauthorized("An authenticated gateway identity is required");
        }
        return actor;
    }

    private Scope scope(String id, ScopeType type) {
        try {
            return new Scope(id, type);
        } catch (IllegalArgumentException exception) {
            throw unauthorized("The authenticated actor does not contain a valid " + type + " scope");
        }
    }

    private UnauthorizedScopeException unauthorized(String message) {
        return new UnauthorizedScopeException(message);
    }
}
