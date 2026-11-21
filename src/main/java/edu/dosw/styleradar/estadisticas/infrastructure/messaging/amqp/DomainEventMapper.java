package edu.dosw.styleradar.estadisticas.infrastructure.messaging.amqp;

import edu.dosw.styleradar.estadisticas.domain.model.CategoryCode;
import edu.dosw.styleradar.estadisticas.domain.model.DomainAnalyticsEvent;
import edu.dosw.styleradar.estadisticas.domain.model.DomainEventType;
import edu.dosw.styleradar.estadisticas.domain.model.PaymentStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Objects;

@Component
public class DomainEventMapper {

    public DomainAnalyticsEvent toDomainEvent(DomainEventPayload payload, String routingKey) {
        Objects.requireNonNull(payload, "Domain event payload must not be null");
        String eventId = required(payload.eventId(), "eventId");
        DomainEventType eventType = resolveEventType(payload.eventType(), routingKey);
        Instant occurredAt = Objects.requireNonNull(payload.occurredAt(), "occurredAt must not be null")
                .toInstant(ZoneOffset.UTC);

        return new DomainAnalyticsEvent(
                eventId,
                eventType,
                occurredAt,
                normalizeOptional(payload.scopeId()),
                normalizeCategory(payload.categoryCode()),
                normalizeCategory(payload.previousCategoryCode()),
                normalizeOrigin(payload.origin()),
                normalizeOptional(payload.itemId()),
                normalizeOptional(payload.itemName()),
                payload.amount(),
                payload.delta() != null ? payload.delta() : 0L,
                normalizePaymentStatus(payload.paymentStatus()));
    }

    private DomainEventType resolveEventType(String rawEventType, String routingKey) {
        if (rawEventType != null && !rawEventType.isBlank()) {
            try {
                return DomainEventType.valueOf(rawEventType.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Unsupported domain event type: " + rawEventType, ex);
            }
        }
        if (routingKey == null || routingKey.isBlank()) {
            throw new IllegalArgumentException("eventType or routingKey must be provided");
        }
        String normalizedKey = routingKey.trim().toLowerCase(Locale.ROOT);
        if (normalizedKey.startsWith("styleradar.usuarios.sesion")) {
            return DomainEventType.USER_SESSION_CHANGED;
        }
        if (normalizedKey.startsWith("styleradar.usuarios.fundacion")) {
            return DomainEventType.FOUNDATION_STATUS_CHANGED;
        }
        if (normalizedKey.startsWith("styleradar.tiendas.visita")) {
            return DomainEventType.STORE_VISIT_REGISTERED;
        }
        if (normalizedKey.startsWith("styleradar.tiendas.verificacion")) {
            return DomainEventType.STORE_VERIFICATION_CHANGED;
        }
        if (normalizedKey.startsWith("styleradar.inventario.stock")) {
            return DomainEventType.INVENTORY_STATE_CHANGED;
        }
        if (normalizedKey.startsWith("styleradar.catalogo.prenda.moderacion")) {
            return DomainEventType.GARMENT_MODERATION_PENDING;
        }
        if (normalizedKey.startsWith("styleradar.catalogo.prenda")) {
            return DomainEventType.GARMENT_PUBLISHED;
        }
        if (normalizedKey.startsWith("styleradar.catalogo.busqueda")) {
            return DomainEventType.CATALOG_SEARCH_EXECUTED;
        }
        if (normalizedKey.startsWith("styleradar.playlist.creada")) {
            return DomainEventType.PLAYLIST_CREATED;
        }
        if (normalizedKey.startsWith("styleradar.playlist.prenda")) {
            return DomainEventType.GARMENT_SAVED_IN_PLAYLIST;
        }
        if (normalizedKey.startsWith("styleradar.chat.mensaje")) {
            return DomainEventType.STORE_MESSAGE_RECEIVED;
        }
        if (normalizedKey.startsWith("styleradar.pagos.suscripcion.pago")) {
            return DomainEventType.SUBSCRIPTION_PAYMENT_PROCESSED;
        }
        if (normalizedKey.startsWith("styleradar.pagos.suscripcion.plan")) {
            return DomainEventType.SUBSCRIPTION_PLAN_CHANGED;
        }
        if (normalizedKey.startsWith("styleradar.donaciones.solicitud.creada")) {
            return DomainEventType.DONATION_REQUEST_CREATED;
        }
        if (normalizedKey.startsWith("styleradar.donaciones.solicitud.estado")) {
            return DomainEventType.DONATION_REQUEST_STATE_CHANGED;
        }
        if (normalizedKey.startsWith("styleradar.donaciones.completada")) {
            return DomainEventType.DONATION_COMPLETED;
        }
        throw new IllegalArgumentException("Unsupported routing key: " + routingKey);
    }

    private CategoryCode normalizeCategory(String rawCategory) {
        if (rawCategory == null || rawCategory.isBlank()) {
            return null;
        }
        String normalized = rawCategory.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "TIENDA", "STORE", "ORIGIN_STORE" -> CategoryCode.ORIGIN_STORE;
            case "USUARIO", "USER", "PERSONA", "ORIGIN_USER" -> CategoryCode.ORIGIN_USER;
            case "PLAN_TIENDA", "PLAN_STORE_MONTHLY" -> CategoryCode.PLAN_STORE_MONTHLY;
            case "APROBADA", "APROBADO", "VERIFICADA", "APPROVED" -> CategoryCode.APPROVED;
            case "PENDIENTE", "PENDING" -> CategoryCode.PENDING;
            default -> new CategoryCode(normalized);
        };
    }

    private CategoryCode normalizeOrigin(String rawOrigin) {
        if (rawOrigin == null || rawOrigin.isBlank()) {
            return null;
        }
        return switch (rawOrigin.trim().toUpperCase(Locale.ROOT)) {
            case "TIENDA", "STORE", "ORIGIN_STORE" -> CategoryCode.ORIGIN_STORE;
            case "USUARIO", "USER", "PERSONA", "ORIGIN_USER" -> CategoryCode.ORIGIN_USER;
            default -> throw new IllegalArgumentException("Unsupported origin: " + rawOrigin);
        };
    }

    private PaymentStatus normalizePaymentStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }
        String normalized = rawStatus.trim().toUpperCase(Locale.ROOT);
        if ("APPROVED".equals(normalized)) {
            return PaymentStatus.APROBADO;
        }
        if ("REJECTED".equals(normalized)) {
            return PaymentStatus.RECHAZADO;
        }
        if ("PENDING".equals(normalized)) {
            return PaymentStatus.PENDIENTE;
        }
        return PaymentStatus.valueOf(normalized);
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
