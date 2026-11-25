package edu.dosw.styleradar.estadisticas.infrastructure.messaging.amqp;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DomainEventPayload(
        @JsonAlias({"eventoId", "id"})
        String eventId,
        @JsonAlias({"tipoEvento", "type"})
        String eventType,
        @JsonAlias({"fechaCreacion", "fechaEvento", "timestamp", "occurredOn"})
        String occurredAt,
        @JsonAlias({"storeId", "tiendaId", "almacenId", "sedeId", "foundationId", "fundacionId"})
        String scopeId,
        @JsonAlias({"estado", "plan", "planActual", "rol", "role", "categoria", "category"})
        String categoryCode,
        @JsonAlias({"estadoAnterior", "planAnterior", "previousCategory"})
        String previousCategoryCode,
        @JsonAlias({"monto", "cantidad", "quantity", "value"})
        BigDecimal amount,
        Long delta,
        @JsonAlias({"estadoPago"})
        String paymentStatus,
        DomainEventData data
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DomainEventData(
            String tiendaId,
            String almacenId,
            String sedeId,
            String fundacionId,
            String foundationId,
            Integer stockAnterior,
            Integer stockDisponible,
            String estado,
            BigDecimal monto,
            BigDecimal cantidad,
            String rol,
            String role,
            Integer planId,
            String tipoSuscriptor,
            String planAnterior,
            String planActual
    ) {
    }
}
