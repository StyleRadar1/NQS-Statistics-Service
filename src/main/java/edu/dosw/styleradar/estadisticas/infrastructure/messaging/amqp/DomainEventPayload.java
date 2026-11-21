package edu.dosw.styleradar.estadisticas.infrastructure.messaging.amqp;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DomainEventPayload(
        @JsonAlias({"eventoId", "id"})
        String eventId,
        @JsonAlias({"tipoEvento", "type"})
        String eventType,
        @JsonAlias({"fechaCreacion", "fechaEvento", "timestamp"})
        LocalDateTime occurredAt,
        @JsonAlias({"storeId", "tiendaId", "foundationId", "fundacionId"})
        String scopeId,
        @JsonAlias({"estado", "plan", "tipoPrenda", "category"})
        String categoryCode,
        @JsonAlias({"estadoAnterior", "planAnterior", "previousCategory"})
        String previousCategoryCode,
        @JsonAlias({"origen", "tipoPublicador", "publisherType"})
        String origin,
        @JsonAlias({"prendaId", "garmentId", "publicadorId", "publisherId"})
        String itemId,
        @JsonAlias({"prendaNombre", "garmentName", "publicadorNombre", "publisherName"})
        String itemName,
        @JsonAlias({"monto", "cantidad", "quantity", "value"})
        BigDecimal amount,
        Long delta,
        @JsonAlias({"estadoPago"})
        String paymentStatus
) {
}
