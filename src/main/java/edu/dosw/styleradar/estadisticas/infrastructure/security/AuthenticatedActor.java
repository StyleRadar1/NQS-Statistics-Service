package edu.dosw.styleradar.estadisticas.infrastructure.security;

public record AuthenticatedActor(String role, String storeId, String foundationId) {
}
