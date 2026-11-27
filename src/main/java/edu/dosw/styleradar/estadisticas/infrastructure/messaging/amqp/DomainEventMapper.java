package edu.dosw.styleradar.estadisticas.infrastructure.messaging.amqp;

import edu.dosw.styleradar.estadisticas.domain.model.CategoryCode;
import edu.dosw.styleradar.estadisticas.domain.model.DomainAnalyticsEvent;
import edu.dosw.styleradar.estadisticas.domain.model.DomainEventType;
import edu.dosw.styleradar.estadisticas.domain.model.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class DomainEventMapper {

    private static final Logger log = LoggerFactory.getLogger(DomainEventMapper.class);

    public static final String USER_REGISTERED_ROUTING_KEY = "styleradar.usuarios.usuario.registrado";
    public static final String INVENTORY_UPDATED_ROUTING_KEY = "styleradar.inventario.actualizado";
    public static final String INVENTORY_LEGACY_UPDATED_ROUTING_KEY = "styleradar.inventario.stock.actualizado";
    public static final String GARMENT_PUBLISHED_ROUTING_KEY = "styleradar.catalogo.prenda.publicada";
    public static final String PLAYLIST_CREATED_ROUTING_KEY = "styleradar.playlist.creada";
    public static final String SUBSCRIPTION_PAYMENT_CONFIRMED_ROUTING_KEY = "styleradar.pagos.pago.confirmado";
    public static final String SUBSCRIPTION_PLAN_UPDATED_ROUTING_KEY = "styleradar.pagos.suscripcion.plan.actualizado";
    public static final String DONATION_COMPLETED_ROUTING_KEY = "styleradar.donaciones.completada";

    public boolean isSupportedRoutingKey(String routingKey) {
        return resolveByRoutingKey(normalizeRoutingKey(routingKey)) != null;
    }

    public DomainAnalyticsEvent toDomainEvent(DomainEventPayload payload, String routingKey) {
        Objects.requireNonNull(payload, "Domain event payload must not be null");
        DomainEventPayload.DomainEventData eventData = payload.data();

        String eventId = required(
                firstNonBlank(payload.eventId(), eventData != null ? eventData.eventId() : null),
                "eventId");
        DomainEventType eventType = resolveEventType(payload.eventType(), routingKey);
        Instant occurredAt = parseOccurredAt(
                firstNonBlank(payload.occurredAt(), eventData != null ? eventData.occurredAt() : null));

        String scopeId = resolveScopeId(payload, eventData);
        CategoryCode categoryCode = null;
        CategoryCode previousCategoryCode = null;
        BigDecimal amount = null;
        PaymentStatus paymentStatus = null;
        String subscriberType = null;

        switch (eventType) {
            case USER_REGISTERED -> {
                String rawRole = resolveRawRole(payload, eventData);
                categoryCode = isBuyerRole(rawRole) ? CategoryCode.PLAN_RADAR : null;
            }
            case INVENTORY_STATE_CHANGED -> {
                Integer stockDisponible = eventData != null && eventData.stockDisponible() != null
                        ? eventData.stockDisponible()
                        : payload.stockDisponible();
                Integer stockAnterior = eventData != null && eventData.stockAnterior() != null
                        ? eventData.stockAnterior()
                        : payload.stockAnterior();

                if (stockDisponible != null || eventData != null) {
                    previousCategoryCode = stockAnterior == null
                            ? null
                            : categoryForStock(requiredStock(stockAnterior, "stockAnterior"));
                    categoryCode = categoryForStock(requiredStock(stockDisponible, "stockDisponible"));
                } else {
                    previousCategoryCode = normalizeInventoryCategory(payload.previousCategoryCode());
                    categoryCode = normalizeInventoryCategory(firstNonBlank(payload.categoryCode(), payload.estado()));
                }
            }
            case SUBSCRIPTION_PAYMENT_PROCESSED -> {
                amount = firstNonNull(
                        eventData != null ? eventData.monto() : null,
                        firstNonNull(payload.monto(), payload.amount()));
                String rawStatus = firstNonBlank(
                        payload.paymentStatus(),
                        firstNonBlank(
                                eventData != null ? firstNonBlank(eventData.estadoPago(), eventData.estado()) : null,
                                firstNonBlank(payload.estado(), payload.categoryCode())));
                if (rawStatus != null) {
                    paymentStatus = normalizePaymentStatus(rawStatus);
                } else if (isConfirmedPaymentRoute(payload.eventType(), routingKey)) {
                    paymentStatus = PaymentStatus.APROBADO;
                }
            }
            case SUBSCRIPTION_PLAN_CHANGED -> {
                subscriberType = normalizeOptional(firstNonBlank(
                        eventData != null ? eventData.tipoSuscriptor() : null,
                        firstNonBlank(payload.subscriberType(),
                                firstNonBlank(eventData != null ? firstNonBlank(eventData.rol(), eventData.role()) : null,
                                        payload.rol()))));

                String rawCurrentPlan = firstNonBlank(
                        eventData != null ? eventData.planActual() : null,
                        firstNonBlank(payload.planActual(), payload.categoryCode()));
                if (rawCurrentPlan == null) {
                    String rawState = firstNonBlank(
                            eventData != null ? eventData.estado() : null,
                            payload.estado());
                    Integer rawPlanId = eventData != null && eventData.planId() != null
                            ? eventData.planId()
                            : payload.planId();
                    if (isSuspendedAlias(rawState)) {
                        rawCurrentPlan = CategoryCode.SUSPENDED.value();
                    } else if (rawPlanId != null) {
                        rawCurrentPlan = String.valueOf(rawPlanId);
                    }
                }

                String rawPreviousPlan = firstNonBlank(
                        eventData != null ? eventData.planAnterior() : null,
                        firstNonBlank(payload.planAnterior(), payload.previousCategoryCode()));
                if (rawPreviousPlan == null) {
                    Integer rawPrevPlanId = eventData != null && eventData.planAnteriorId() != null
                            ? eventData.planAnteriorId()
                            : payload.planAnteriorId();
                    if (rawPrevPlanId != null) {
                        rawPreviousPlan = String.valueOf(rawPrevPlanId);
                    }
                }

                categoryCode = normalizePlanCategory(rawCurrentPlan);
                previousCategoryCode = normalizePlanCategory(rawPreviousPlan);
            }
            case GARMENT_PUBLISHED, PLAYLIST_CREATED, DONATION_COMPLETED -> amount = firstNonNull(
                    eventData != null ? eventData.cantidad() : null,
                    payload.amount());
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
                paymentStatus,
                subscriberType);
    }

    private String resolveScopeId(DomainEventPayload payload, DomainEventPayload.DomainEventData eventData) {
        if (eventData == null) {
            return payload.scopeId();
        }
        return firstNonBlank(
                eventData.tiendaId(),
                firstNonBlank(
                        payload.scopeId(),
                        firstNonBlank(
                                eventData.almacenId(),
                                firstNonBlank(
                                        eventData.sedeId(),
                                        firstNonBlank(eventData.fundacionId(), eventData.foundationId())))));
    }

    private String resolveRawRole(DomainEventPayload payload, DomainEventPayload.DomainEventData eventData) {
        String directRole = firstNonBlank(
                eventData != null ? firstNonBlank(eventData.rol(), eventData.role()) : null,
                firstNonBlank(payload.rol(), payload.categoryCode()));
        if (directRole != null) {
            return directRole;
        }
        List<String> roles = eventData != null ? eventData.roles() : null;
        if (roles != null) {
            for (String candidate : roles) {
                if (candidate != null && !candidate.isBlank()) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private boolean isConfirmedPaymentRoute(String rawEventType, String routingKey) {
        return SUBSCRIPTION_PAYMENT_CONFIRMED_ROUTING_KEY.equals(normalizeRoutingKey(routingKey))
                || SUBSCRIPTION_PAYMENT_CONFIRMED_ROUTING_KEY.equals(normalizeRoutingKey(rawEventType));
    }

    private DomainEventType resolveEventType(String rawEventType, String routingKey) {
        if (routingKey != null && !routingKey.isBlank()) {
            DomainEventType byHeaderRouting = resolveByRoutingKey(normalizeRoutingKey(routingKey));
            if (byHeaderRouting != null) {
                return byHeaderRouting;
            }
        }
        if (rawEventType != null && !rawEventType.isBlank()) {
            DomainEventType byType = resolveByEventTypeOrAlias(rawEventType);
            if (byType != null) {
                return byType;
            }
            throw new IllegalArgumentException("Unsupported domain event type: " + rawEventType);
        }
        if (routingKey != null && !routingKey.isBlank()) {
            throw new IllegalArgumentException("Unsupported routing key: " + routingKey);
        }
        throw new IllegalArgumentException("eventType or routingKey must be provided");
    }

    private DomainEventType resolveByEventTypeOrAlias(String rawEventType) {
        String trimmed = rawEventType.trim();
        try {
            return DomainEventType.valueOf(trimmed.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            // Fall through to routing keys and producer aliases
        }
        DomainEventType byRouting = resolveByRoutingKey(normalizeRoutingKey(trimmed));
        if (byRouting != null) {
            return byRouting;
        }
        return switch (trimmed.toUpperCase(Locale.ROOT)) {
            case "USUARIO_REGISTRADO", "USER_CREATED", "USERREGISTRATIONEVENT" -> DomainEventType.USER_REGISTERED;
            case "INVENTARIO_ACTUALIZADO", "INVENTORY_UPDATED", "STOCK_UPDATED" -> DomainEventType.INVENTORY_STATE_CHANGED;
            case "PRENDA_PUBLICADA", "ITEM_PUBLISHED" -> DomainEventType.GARMENT_PUBLISHED;
            case "PLAYLIST_CREADA" -> DomainEventType.PLAYLIST_CREATED;
            case "PAGO_CONFIRMADO", "PAYMENT_CONFIRMED", "SUBSCRIPTION_PAYMENT_CONFIRMED", "PAGOCONFIRMADOEVENT" ->
                    DomainEventType.SUBSCRIPTION_PAYMENT_PROCESSED;
            case "SUSCRIPCION_PLAN_ACTUALIZADO", "SUBSCRIPTION_PLAN_UPDATED", "PLAN_ACTUALIZADO", "PLAN_UPDATED",
                    "SUSCRIPCIONPLANACTUALIZADOEVENT" -> DomainEventType.SUBSCRIPTION_PLAN_CHANGED;
            case "DONACION_COMPLETADA", "DONATIONDELIVEREDEVENT", "DONATION_DELIVERED", "DONACION_ENTREGADA" ->
                    DomainEventType.DONATION_COMPLETED;
            default -> null;
        };
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
        String normalized = stripRolePrefix(rawRole);
        return switch (normalized) {
            case "BUYER", "COMPRADOR", "USUARIO", "USER", "CLIENTE" -> true;
            default -> false;
        };
    }

    private String stripRolePrefix(String rawRole) {
        String normalized = rawRole.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized.substring("ROLE_".length()) : normalized;
    }

    private String normalizeRoutingKey(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private Instant parseOccurredAt(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("occurredAt must not be null");
        }
        String trimmed = value.trim();
        try {
            return Instant.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            // Try other supported ISO-8601 and epoch representations
        }
        try {
            return OffsetDateTime.parse(trimmed).toInstant();
        } catch (DateTimeParseException ignored) {
            // Continue
        }
        try {
            return ZonedDateTime.parse(trimmed).toInstant();
        } catch (DateTimeParseException ignored) {
            // Continue
        }
        try {
            return LocalDateTime.parse(trimmed.replace(' ', 'T')).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            // Continue
        }
        try {
            return LocalDate.parse(trimmed).atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (DateTimeParseException ignored) {
            // Continue
        }
        try {
            BigDecimal numeric = new BigDecimal(trimmed);
            if (numeric.abs().compareTo(BigDecimal.valueOf(100_000_000_000L)) >= 0) {
                return Instant.ofEpochMilli(numeric.longValue());
            }
            long seconds = numeric.longValue();
            long nanos = numeric.remainder(BigDecimal.ONE).movePointRight(9).longValue();
            return Instant.ofEpochSecond(seconds, nanos);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid ISO-8601 timestamp: " + value, ex);
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

    private CategoryCode normalizeInventoryCategory(String rawCategory) {
        if (rawCategory == null || rawCategory.isBlank()) {
            return null;
        }
        return switch (rawCategory.trim().toUpperCase(Locale.ROOT)) {
            case "DISPONIBLE", "NUEVA_PRENDA", "AVAILABLE" -> CategoryCode.AVAILABLE;
            case "ULTIMAS_UNIDADES", "LOW_STOCK" -> CategoryCode.LOW_STOCK;
            case "AGOTADA", "AGOTADO", "OUT_OF_STOCK" -> CategoryCode.OUT_OF_STOCK;
            default -> throw new IllegalArgumentException("Unsupported inventory state category: " + rawCategory);
        };
    }

    private CategoryCode normalizePlanCategory(String rawPlan) {
        if (rawPlan == null || rawPlan.isBlank()) {
            return null;
        }
        String normalized = rawPlan.trim().toUpperCase(Locale.ROOT);
        if (isSuspendedAlias(normalized)) {
            return CategoryCode.SUSPENDED;
        }
        return switch (normalized) {
            case "1", "RADAR", "PLAN_RADAR", "FREE", "GRATIS", "BASICO", "BASIC", "PLAN_BASICO", "PLAN_FREE" ->
                    CategoryCode.PLAN_RADAR;
            case "2", "RADAR+", "RADAR_PLUS", "PLAN_RADAR_PLUS", "PLUS", "PREMIUM", "PLAN_PREMIUM" ->
                    CategoryCode.PLAN_RADAR_PLUS;
            case "3", "ALMACEN", "STORE", "TIENDA", "PLAN_ALMACEN", "PLAN_TIENDA",
                    "STORE_MONTHLY", "MENSUAL_TIENDA", "PLAN_STORE", "PLAN_STORE_MONTHLY", "MENSUAL" ->
                    CategoryCode.PLAN_STORE_MONTHLY;
            default -> throw new IllegalArgumentException("Unsupported subscription plan category: " + rawPlan);
        };
    }

    private boolean isSuspendedAlias(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return false;
        }
        return switch (rawStatus.trim().toUpperCase(Locale.ROOT)) {
            case "SUSPENDED", "SUSPENDIDO", "SUSPENDIDA", "INACTIVE", "INACTIVO", "INACTIVA",
                    "EXPIRED", "VENCIDO", "VENCIDA", "CANCELLED", "CANCELED", "CANCELADO", "CANCELADA" -> true;
            default -> false;
        };
    }

    private PaymentStatus normalizePaymentStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }
        String normalized = rawStatus.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "APPROVED", "APROBADO", "APROBADA", "CONFIRMED", "CONFIRMADO", "CONFIRMADA",
                    "COMPLETED", "COMPLETADO", "COMPLETADA", "PAID", "PAGADO", "PAGADA",
                    "SUCCESS", "SUCCESSFUL", "EXITOSO", "EXITOSA", "ACTIVA", "ACTIVO", "ACTIVE" ->
                    PaymentStatus.APROBADO;
            case "REJECTED", "RECHAZADO", "RECHAZADA", "DECLINED", "DECLINADO",
                    "FAILED", "FALLIDO", "FALLIDA", "CANCELLED", "CANCELED", "CANCELADO", "CANCELADA",
                    "VOIDED", "ANULADO", "ANULADA", "EXPIRED", "VENCIDO", "VENCIDA" ->
                    PaymentStatus.RECHAZADO;
            case "PENDING", "PENDIENTE", "PROCESSING", "EN_PROCESO", "IN_PROGRESS", "CREATED", "CREADO" ->
                    PaymentStatus.PENDIENTE;
            case "REFUNDED", "REEMBOLSADO", "REEMBOLSADA", "REVERSED", "REVERSADO" ->
                    PaymentStatus.REEMBOLSADO;
            default -> {
                log.warn("Estado de pago no reconocido '{}'; se marca como no aprobado para ingresos", rawStatus);
                yield PaymentStatus.RECHAZADO;
            }
        };
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
