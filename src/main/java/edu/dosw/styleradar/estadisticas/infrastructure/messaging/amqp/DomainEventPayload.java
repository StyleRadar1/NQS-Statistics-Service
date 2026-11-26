package edu.dosw.styleradar.estadisticas.infrastructure.messaging.amqp;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DomainEventPayload(
        @JsonAlias({"eventoId", "id", "event_id", "evento_id", "messageId"})
        String eventId,
        @JsonAlias({"tipoEvento", "event_type", "type"})
        String eventType,
        @JsonDeserialize(using = FlexibleTimestampDeserializer.class)
        @JsonAlias({
                "fechaCreacion", "fechaEvento", "timestamp", "occurredOn",
                "occurred_at", "fecha", "createdAt", "fechaPago", "fechaPublicacion", "fechaRegistro"
        })
        String occurredAt,
        @JsonAlias({"storeId", "tiendaId", "almacenId", "sedeId", "foundationId", "fundacionId", "idTienda", "idFundacion", "nit"})
        String scopeId,
        @JsonAlias({"categoria", "category"})
        String categoryCode,
        @JsonAlias({"estadoAnterior", "previousCategory"})
        String previousCategoryCode,
        @JsonAlias({"cantidad", "quantity", "value"})
        BigDecimal amount,
        Long delta,
        @JsonAlias({"estadoPago", "payment_status", "estadoTransaccion", "transactionStatus"})
        String paymentStatus,
        DomainEventData data,
        @JsonAlias({"valor", "valorPagado", "montoTotal", "montoPagado", "total", "precio"})
        BigDecimal monto,
        @JsonAlias({"status", "estadoSuscripcion"})
        String estado,
        @JsonAlias({"role"})
        String rol,
        @JsonAlias({"tipoSuscriptor", "tipo_suscriptor", "tipoUsuario", "tipoCuenta"})
        String subscriberType,
        @JsonAlias({"previousPlan", "plan_anterior", "oldPlan", "planPrevio"})
        String planAnterior,
        @JsonAlias({"plan", "currentPlan", "plan_actual", "newPlan", "nuevoPlan", "planNuevo", "codigoPlan", "planCode"})
        String planActual,
        @JsonAlias({"plan_id", "idPlan", "planActualId"})
        Integer planId,
        @JsonAlias({"plan_anterior_id", "previousPlanId", "idPlanAnterior"})
        Integer planAnteriorId,
        @JsonAlias({"previousStock", "stock_anterior"})
        Integer stockAnterior,
        @JsonAlias({"stockActual", "nuevoStock", "availableStock", "stock_disponible", "stock"})
        Integer stockDisponible
) {

    public DomainEventPayload(
            String eventId,
            String eventType,
            String occurredAt,
            String scopeId,
            String categoryCode,
            String previousCategoryCode,
            BigDecimal amount,
            Long delta,
            String paymentStatus,
            DomainEventData data
    ) {
        this(eventId, eventType, occurredAt, scopeId, categoryCode, previousCategoryCode,
                amount, delta, paymentStatus, data, null, null, null, null, null, null, null, null, null, null);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DomainEventData(
            @JsonAlias({"storeId", "idTienda", "store_id", "tienda_id", "nit"})
            String tiendaId,
            @JsonAlias({"almacen_id", "idAlmacen"})
            String almacenId,
            @JsonAlias({"sede_id", "idSede", "branchId"})
            String sedeId,
            @JsonAlias({"idFundacion", "fundacion_id"})
            String fundacionId,
            @JsonAlias({"foundation_id"})
            String foundationId,
            @JsonAlias({"previousStock", "stock_anterior", "cantidadAnterior"})
            Integer stockAnterior,
            @JsonAlias({"stockActual", "nuevoStock", "availableStock", "stock_disponible", "cantidadActual", "stock"})
            Integer stockDisponible,
            @JsonAlias({"status", "subscriptionStatus", "estadoSuscripcion"})
            String estado,
            @JsonAlias({"amount", "valor", "valorPagado", "montoTotal", "montoPagado", "total", "monto_total", "valor_pagado"})
            BigDecimal monto,
            @JsonAlias({"quantity", "cantidadPrendas", "prendasRecibidas", "totalPrendas"})
            BigDecimal cantidad,
            String rol,
            String role,
            @JsonAlias({"plan_id", "idPlan", "planActualId"})
            Integer planId,
            @JsonAlias({"subscriberType", "subscriber_type", "tipo_suscriptor", "tipoUsuario", "tipoCuenta"})
            String tipoSuscriptor,
            @JsonAlias({"previousPlan", "plan_anterior", "oldPlan", "planPrevio", "codigoPlanAnterior"})
            String planAnterior,
            @JsonAlias({"currentPlan", "plan_actual", "newPlan", "nuevoPlan", "planNuevo", "plan", "codigoPlan", "planCode"})
            String planActual,
            @JsonAlias({"eventoId", "event_id", "evento_id"})
            String eventId,
            @JsonDeserialize(using = FlexibleTimestampDeserializer.class)
            @JsonAlias({"timestamp", "fechaEvento", "fechaCreacion", "occurredOn", "fechaPago", "fechaPublicacion"})
            String occurredAt,
            @JsonAlias({"paymentStatus", "payment_status", "estadoTransaccion", "transactionStatus", "estado_pago"})
            String estadoPago,
            @JsonAlias({"plan_anterior_id", "previousPlanId", "idPlanAnterior"})
            Integer planAnteriorId,
            List<String> roles
    ) {
        public DomainEventData(
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
            this(tiendaId, almacenId, sedeId, fundacionId, foundationId, stockAnterior, stockDisponible,
                    estado, monto, cantidad, rol, role, planId, tipoSuscriptor, planAnterior, planActual,
                    null, null, null, null, null);
        }
    }

    static final class FlexibleTimestampDeserializer extends JsonDeserializer<String> {

        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonToken token = parser.currentToken();
            if (token == JsonToken.VALUE_NULL) {
                return null;
            }
            if (token == JsonToken.VALUE_STRING) {
                return parser.getText();
            }
            if (token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT) {
                return epochDecimalToIso(parser.getDecimalValue());
            }
            if (token == JsonToken.START_ARRAY) {
                List<Integer> parts = new ArrayList<>(7);
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    parts.add(parser.getIntValue());
                }
                return arrayToIso(parts);
            }
            return parser.getValueAsString();
        }

        private static String epochDecimalToIso(BigDecimal numeric) {
            if (numeric.abs().compareTo(BigDecimal.valueOf(100_000_000_000L)) >= 0) {
                return Instant.ofEpochMilli(numeric.longValue()).toString();
            }
            long seconds = numeric.longValue();
            long nanos = numeric.remainder(BigDecimal.ONE).movePointRight(9).longValue();
            return Instant.ofEpochSecond(seconds, nanos).toString();
        }

        private static String arrayToIso(List<Integer> parts) {
            if (parts.size() < 3) {
                throw new IllegalArgumentException("Invalid timestamp array: " + parts);
            }
            int year = parts.get(0);
            int month = parts.get(1);
            int day = parts.get(2);
            if (parts.size() == 3) {
                return LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toInstant().toString();
            }
            int hour = parts.get(3);
            int minute = parts.size() > 4 ? parts.get(4) : 0;
            int second = parts.size() > 5 ? parts.get(5) : 0;
            int nano = parts.size() > 6 ? parts.get(6) : 0;
            return LocalDateTime.of(year, month, day, hour, minute, second, nano)
                    .toInstant(ZoneOffset.UTC)
                    .toString();
        }
    }
}
