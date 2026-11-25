package edu.dosw.styleradar.estadisticas.infrastructure.messaging.amqp;

import edu.dosw.styleradar.estadisticas.domain.model.CategoryCode;
import edu.dosw.styleradar.estadisticas.domain.model.DomainAnalyticsEvent;
import edu.dosw.styleradar.estadisticas.domain.model.DomainEventType;
import edu.dosw.styleradar.estadisticas.domain.model.PaymentStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;

@Component
public class DomainEventMapper {

    public static final String USER_REGISTERED_ROUTING_KEY = "styleradar.usuarios.usuario.registrado";
    public static final String INVENTORY_UPDATED_ROUTING_KEY = "styleradar.inventario.actualizado";
    public static final String INVENTORY_LEGACY_UPDATED_ROUTING_KEY = "styleradar.inventario.stock.actualizado";
    public static final String GARMENT_PUBLISHED_ROUTING_KEY = "styleradar.catalogo.prenda.publicada";
    public static final String PLAYLIST_CREATED_ROUTING_KEY = "styleradar.playlist.creada";
    public static final String SUBSCRIPTION_PAYMENT_CONFIRMED_ROUTING_KEY = "styleradar.pagos.pago.confirmado";
    public static final String SUBSCRIPTION_PLAN_UPDATED_ROUTING_KEY = "styleradar.pagos.suscripcion.plan.actualizado";
    public static final String DONATION_COMPLETED_ROUTING_KEY = "styleradar.donaciones.completada";

    public DomainAnalyticsEvent toDomainEvent(DomainEventPayload payload, String routingKey) {
        Objects.requireNonNull(payload, "Domain event payload must not be null");
        String eventId = required(payload.eventId(), "eventId");
        DomainEventType eventType = resolveEventType(payload.eventType(), routingKey);
        Instant occurredAt = parseOccurredAt(payload.occurredAt());
        DomainEventPayload.DomainEventData eventData = payload.data();

        String scopeId = firstNonBlank(payload.scopeId(), eventData == null ? null
                : firstNonBlank(eventData.tiendaId(), firstNonBlank(eventData.almacenId(),
                firstNonBlank(eventData.sedeId(), firstNonBlank(eventData.fundacionId(), eventData.foundationId())))));

        CategoryCode categoryCode = normalizeCategory(firstNonBlank(
                payload.categoryCode(),
                eventData == null ? null : firstNonBlank(eventData.planActual(), firstNonBlank(eventData.rol(), eventData.role()))));
        CategoryCode previousCategoryCode = normalizeCategory(firstNonBlank(
                payload.previousCategoryCode(),
                eventData == null ? null : eventData.planAnterior()));

        if (eventType == DomainEventType.USER_REGISTERED) {
            String rawRole = firstNonBlank(
                    payload.categoryCode(),
                    eventData == null ? null : firstNonBlank(eventData.rol(), eventData.role()));
            categoryCode = isBuyerRole(rawRole) ? CategoryCode.PLAN_RADAR : null;
            previousCategoryCode = null;
        }

        if (eventType == DomainEventType.INVENTORY_STATE_CHANGED && eventData != null) {
            previousCategoryCode = eventData.stockAnterior() == null
                    ? null
                    : categoryForStock(requiredStock(eventData.stockAnterior(), "stockAnterior"));
            categoryCode = categoryForStock(requiredStock(eventData.stockDisponible(), "stockDisponible"));
        }

        BigDecimal amount = firstNonNull(
                payload.amount(),
                eventData == null ? null : firstNonNull(eventData.monto(), eventData.cantidad()));

        PaymentStatus paymentStatus = normalizePaymentStatus(firstNonBlank(
                payload.paymentStatus(),
                eventData == null ? null : eventData.estado()));
        boolean confirmedPayment = SUBSCRIPTION_PAYMENT_CONFIRMED_ROUTING_KEY.equals(normalizeRoutingKey(payload.eventType()))
                || SUBSCRIPTION_PAYMENT_CONFIRMED_ROUTING_KEY.equals(normalizeRoutingKey(routingKey));
        if (eventType == DomainEventType.SUBSCRIPTION_PAYMENT_PROCESSED
                && confirmedPayment
                && paymentStatus == null) {
            paymentStatus = PaymentStatus.APROBADO;
        }

        return new DomainAnalyticsEvent(
                eventId,
                eventType,
                occurredAt,
                normalizeOptional(scopeId),
                categoryCode,
                previousCategoryCode,
                amount,
                payload.delta() != null ? payload.delta() : 0L,
                paymentStatus);
    }

    private DomainEventType resolveEventType(String rawEventType, String routingKey) {
        if (rawEventType != null && !rawEventType.isBlank()) {
            try {
                return DomainEventType.valueOf(rawEventType.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                DomainEventType byRouting = resolveByRoutingKey(normalizeRoutingKey(rawEventType));
                if (byRouting != null) {
                    return byRouting;
                }
                throw new IllegalArgumentException("Unsupported domain event type: " + rawEventType, ex);
            }
        }
        if (routingKey == null || routingKey.isBlank()) {
            throw new IllegalArgumentException("eventType or routingKey must be provided");
        }
        DomainEventType resolved = resolveByRoutingKey(normalizeRoutingKey(routingKey));
        if (resolved == null) {
            throw new IllegalArgumentException("Unsupported routing key: " + routingKey);
        }
        return resolved;
    }

    private DomainEventType resolveByRoutingKey(String normalizedKey) {
        if (normalizedKey == null) {
            return null;
        }
        return switch (normalizedKey) {
            case USER_REGISTERED_ROUTING_KEY -> DomainEventType.USER_REGISTERED;
            case INVENTORY_UPDATED_ROUTING_KEY, INVENTORY_LEGACY_UPDATED_ROUTING_KEY -> DomainEventType.INVENTORY_STATE_CHANGED;
            case GARMENT_PUBLISHED_ROUTING_KEY -> DomainEventType.GARMENT_PUBLISHED;
            case PLAYLIST_CREATED_ROUTING_KEY -> DomainEventType.PLAYLIST_CREATED;
            case SUBSCRIPTION_PAYMENT_CONFIRMED_ROUTING_KEY -> DomainEventType.SUBSCRIPTION_PAYMENT_PROCESSED;
            case SUBSCRIPTION_PLAN_UPDATED_ROUTING_KEY -> DomainEventType.SUBSCRIPTION_PLAN_CHANGED;
            case DONATION_COMPLETED_ROUTING_KEY -> DomainEventType.DONATION_COMPLETED;
            default -> null;
        };
    }

    private boolean isBuyerRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return false;
        }
        String normalized = rawRole.trim().toUpperCase(Locale.ROOT);
        return "BUYER".equals(normalized) || "COMPRADOR".equals(normalized);
    }

    private String normalizeRoutingKey(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private Instant parseOccurredAt(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("occurredAt must not be null");
        }
        try {
            return OffsetDateTime.parse(value.trim()).toInstant();
        } catch (DateTimeParseException offsetException) {
            try {
                return LocalDateTime.parse(value.trim()).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException localException) {
                throw new IllegalArgumentException("Invalid ISO-8601 timestamp: " + value, localException);
            }
        }
    }

    private Integer requiredStock(Integer stock, String field) {
        if (stock == null || stock < 0) {
            throw new IllegalArgumentException(field + " must be a non-negative number");
        }
        return stock;
    }

    private CategoryCode categoryForStock(Integer stock) {
        if (stock == 0) {
            return CategoryCode.OUT_OF_STOCK;
        }
        if (stock <= 3) {
            return CategoryCode.LOW_STOCK;
        }
        return CategoryCode.AVAILABLE;
    }

    private CategoryCode normalizeCategory(String rawCategory) {
        if (rawCategory == null || rawCategory.isBlank()) {
            return null;
        }
        String normalized = rawCategory.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "DISPONIBLE", "NUEVA_PRENDA", "AVAILABLE" -> CategoryCode.AVAILABLE;
            case "ULTIMAS_UNIDADES", "LOW_STOCK" -> CategoryCode.LOW_STOCK;
            case "AGOTADA", "AGOTADO", "OUT_OF_STOCK" -> CategoryCode.OUT_OF_STOCK;
            case "RADAR", "PLAN_RADAR" -> CategoryCode.PLAN_RADAR;
            case "RADAR+", "RADAR_PLUS", "PLAN_RADAR_PLUS" -> CategoryCode.PLAN_RADAR_PLUS;
            case "ALMACEN", "STORE", "PLAN_ALMACEN", "PLAN_TIENDA", "PLAN_STORE_MONTHLY" -> CategoryCode.PLAN_STORE_MONTHLY;
            case "SUSPENDED", "SUSPENDIDO", "SUSPENDIDA" -> CategoryCode.SUSPENDED;
            default -> new CategoryCode(normalized);
        };
    }

    private PaymentStatus normalizePaymentStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }
        String normalized = rawStatus.trim().toUpperCase(Locale.ROOT);
        if ("APPROVED".equals(normalized) || "ACTIVA".equals(normalized)) {
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

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private <T> T firstNonNull(T preferred, T fallback) {
        return preferred != null ? preferred : fallback;
    }
}
