package edu.dosw.styleradar.estadisticas.infrastructure.messaging.amqp;

import edu.dosw.styleradar.estadisticas.domain.model.AnalyticsEvent;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.PaymentStatus;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class EventToMetricMapper {

    public AnalyticsEvent toAnalyticsEvent(AnalyticsEventPayload payload) {
        Objects.requireNonNull(payload, "Analytics event payload must not be null");
        try {
            MetricCode metricCode = MetricCode.valueOf(required(payload.metricCode(), "metricCode"));
            ScopeType scopeType = ScopeType.valueOf(required(payload.scopeType(), "scopeType"));
            PaymentStatus paymentStatus = payload.paymentStatus() == null || payload.paymentStatus().isBlank()
                    ? null
                    : PaymentStatus.valueOf(payload.paymentStatus());
            return new AnalyticsEvent(
                    payload.eventId(),
                    metricCode,
                    new Scope(payload.scopeId(), scopeType),
                    payload.value(),
                    payload.occurredAt(),
                    paymentStatus);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid analytics event payload", exception);
        }
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
