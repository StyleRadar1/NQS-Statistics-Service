package edu.dosw.styleradar.estadisticas.infrastructure.messaging.amqp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnalyticsEventPayload(
        String eventId,
        String metricCode,
        String scopeType,
        String scopeId,
        BigDecimal value,
        Instant occurredAt,
        String paymentStatus
) {
}
