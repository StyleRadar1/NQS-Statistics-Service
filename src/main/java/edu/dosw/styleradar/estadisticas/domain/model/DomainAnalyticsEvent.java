package edu.dosw.styleradar.estadisticas.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record DomainAnalyticsEvent(
        String eventId,
        DomainEventType eventType,
        Instant occurredAt,
        String scopeId,
        CategoryCode categoryCode,
        CategoryCode previousCategoryCode,
        BigDecimal amount,
        long delta,
        PaymentStatus paymentStatus,
        String subscriberType
) {

    public DomainAnalyticsEvent(
            String eventId,
            DomainEventType eventType,
            Instant occurredAt,
            String scopeId,
            CategoryCode categoryCode,
            CategoryCode previousCategoryCode,
            BigDecimal amount,
            long delta,
            PaymentStatus paymentStatus
    ) {
        this(eventId, eventType, occurredAt, scopeId, categoryCode, previousCategoryCode,
                amount, delta, paymentStatus, null);
    }

    public DomainAnalyticsEvent {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Event id must not be blank");
        }
        Objects.requireNonNull(eventType, "Domain event type must not be null");
        Objects.requireNonNull(occurredAt, "Occurred timestamp must not be null");
        if (amount != null && amount.signum() < 0) {
            throw new IllegalArgumentException("Event amount must not be negative");
        }
    }
}
