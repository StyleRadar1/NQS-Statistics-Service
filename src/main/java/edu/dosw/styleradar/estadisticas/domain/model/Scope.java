package edu.dosw.styleradar.estadisticas.domain.model;

import java.util.Objects;

public record Scope(String id, ScopeType type) {

    public Scope {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Scope id must not be blank");
        }
        Objects.requireNonNull(type, "Scope type must not be null");
    }
}
