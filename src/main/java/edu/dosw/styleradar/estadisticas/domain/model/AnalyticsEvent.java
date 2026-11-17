package edu.dosw.styleradar.estadisticas.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record AnalyticsEvent(
        String eventId,
        MetricCode metricCode,
        Scope scope,
        BigDecimal value,
        Instant occurredAt,
        PaymentStatus paymentStatus
) {

    public AnalyticsEvent {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Event id must not be blank");
        }
        Objects.requireNonNull(metricCode, "Metric code must not be null");
        Objects.requireNonNull(scope, "Scope must not be null");
        Objects.requireNonNull(value, "Event value must not be null");
        Objects.requireNonNull(occurredAt, "Event timestamp must not be null");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("Event value must be positive");
        }
        if (metricCode.scopeType() != scope.type()) {
            throw new IllegalArgumentException("Metric code and scope types must match");
        }
    }
}
