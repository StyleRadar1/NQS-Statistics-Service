package edu.dosw.styleradar.estadisticas.infrastructure.messaging.amqp;

import edu.dosw.styleradar.estadisticas.domain.model.AnalyticsEvent;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.Objects;

@Component
public class EventToMetricMapper {

    private static final String PLATFORM_SCOPE_ID = "platform";

    public AnalyticsEvent toAnalyticsEvent(AlertaCreadaEventPayload payload) {
        Objects.requireNonNull(payload, "AlertaCreada event payload must not be null");
        String eventoId = required(payload.eventoId(), "eventoId");
        Objects.requireNonNull(payload.alertaId(), "alertaId must not be null");
        Objects.requireNonNull(payload.usuarioId(), "usuarioId must not be null");
        required(payload.tipoAlerta(), "tipoAlerta");
        Objects.requireNonNull(payload.fechaCreacion(), "fechaCreacion must not be null");

        return new AnalyticsEvent(
                eventoId,
                MetricCode.PLATFORM_ALERTS_CREATED,
                new Scope(PLATFORM_SCOPE_ID, ScopeType.PLATFORM),
                BigDecimal.ONE,
                payload.fechaCreacion().toInstant(ZoneOffset.UTC),
                null);
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
