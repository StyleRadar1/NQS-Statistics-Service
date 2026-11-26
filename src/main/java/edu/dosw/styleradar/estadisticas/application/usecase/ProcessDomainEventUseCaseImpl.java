package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.model.CategoryCode;
import edu.dosw.styleradar.estadisticas.domain.model.DomainAnalyticsEvent;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.PaymentStatus;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.port.in.ProcessDomainEventUseCase;
import edu.dosw.styleradar.estadisticas.domain.port.out.MetricRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class ProcessDomainEventUseCaseImpl implements ProcessDomainEventUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessDomainEventUseCaseImpl.class);
    private static final Scope PLATFORM_SCOPE = new Scope("platform", ScopeType.PLATFORM);
    private static final ZoneId BOGOTA_ZONE = ZoneId.of("America/Bogota");
    private static final Set<CategoryCode> INVENTORY_CATEGORIES = Set.of(
            CategoryCode.AVAILABLE,
            CategoryCode.LOW_STOCK,
            CategoryCode.OUT_OF_STOCK);
    private static final Set<CategoryCode> BUYER_CATEGORIES = Set.of(
            CategoryCode.PLAN_RADAR,
            CategoryCode.PLAN_RADAR_PLUS);
    private static final Set<CategoryCode> STORE_CATEGORIES = Set.of(
            CategoryCode.PLAN_STORE_MONTHLY,
            CategoryCode.SUSPENDED);

    private final MetricRepositoryPort metricRepository;

    public ProcessDomainEventUseCaseImpl(MetricRepositoryPort metricRepository) {
        this.metricRepository = metricRepository;
    }

    @Override
    @Transactional
    public void process(DomainAnalyticsEvent event) {
        Objects.requireNonNull(event, "Domain analytics event must not be null");

        LocalDate metricDate = event.occurredAt().atZone(BOGOTA_ZONE).toLocalDate();
        BigDecimal positiveAmount = (event.amount() != null && event.amount().signum() > 0)
                ? event.amount()
                : BigDecimal.ONE;

        switch (event.eventType()) {
            case USER_REGISTERED -> {
                metricRepository.upsertDailyIncrement(
                        MetricCode.PLATFORM_USER_REGISTRATIONS, PLATFORM_SCOPE, metricDate, BigDecimal.ONE);
                if (CategoryCode.PLAN_RADAR.equals(event.categoryCode())) {
                    metricRepository.adjustDistributionCount(
                            MetricCode.PLATFORM_BUYERS_BY_PLAN, PLATFORM_SCOPE, CategoryCode.PLAN_RADAR, 1L);
                }
            }
            case INVENTORY_STATE_CHANGED -> {
                Scope storeScope = requireScope(event.scopeId(), ScopeType.STORE);
                CategoryCode currentInventory = requireAllowedCategory(
                        event.categoryCode(), INVENTORY_CATEGORIES, MetricCode.STORE_INVENTORY_BY_STATE);
                CategoryCode previousInventory = event.previousCategoryCode() == null
                        ? null
                        : requireAllowedCategory(
                                event.previousCategoryCode(), INVENTORY_CATEGORIES, MetricCode.STORE_INVENTORY_BY_STATE);
                applyCategoryTransition(
                        MetricCode.STORE_INVENTORY_BY_STATE,
                        storeScope,
                        previousInventory,
                        currentInventory);
            }
            case GARMENT_PUBLISHED -> metricRepository.upsertDailyIncrement(
                    MetricCode.PLATFORM_GARMENT_PUBLICATIONS, PLATFORM_SCOPE, metricDate, positiveAmount);
            case PLAYLIST_CREATED -> metricRepository.upsertDailyIncrement(
                    MetricCode.PLATFORM_PLAYLISTS_CREATED, PLATFORM_SCOPE, metricDate, positiveAmount);
            case SUBSCRIPTION_PAYMENT_PROCESSED -> {
                if (event.paymentStatus() == PaymentStatus.APROBADO) {
                    if (event.amount() != null && event.amount().signum() > 0) {
                        metricRepository.upsertDailyIncrement(
                                MetricCode.PLATFORM_SUBSCRIPTION_REVENUE, PLATFORM_SCOPE, metricDate, event.amount());
                    } else {
                        log.warn("Pago confirmado ignorado para ingresos por no incluir monto positivo: eventoId={}",
                                event.eventId());
                    }
                }
            }
            case SUBSCRIPTION_PLAN_CHANGED -> processSubscriptionPlanChanged(event);
            case DONATION_COMPLETED -> {
                Scope foundationScope = requireScope(event.scopeId(), ScopeType.FOUNDATION);
                metricRepository.upsertDailyIncrement(
                        MetricCode.FOUNDATION_GARMENTS_RECEIVED, foundationScope, metricDate, positiveAmount);
                metricRepository.upsertDailyIncrement(
                        MetricCode.PLATFORM_MONTHLY_DONATIONS, PLATFORM_SCOPE, metricDate, BigDecimal.ONE);
            }
        }
    }

    private void processSubscriptionPlanChanged(DomainAnalyticsEvent event) {
        if (event.categoryCode() == null && event.previousCategoryCode() == null) {
            throw new IllegalArgumentException(
                    "Subscription plan update requires at least planActual or planAnterior");
        }

        MetricCode planMetric = resolvePlanMetric(event);
        CategoryCode previousPlan = event.previousCategoryCode();
        CategoryCode currentPlan = event.categoryCode();

        if (planMetric == MetricCode.PLATFORM_BUYERS_BY_PLAN) {
            previousPlan = normalizeBuyerPlanCategory(previousPlan);
            currentPlan = normalizeBuyerPlanCategory(currentPlan);
            if (currentPlan == null && CategoryCode.PLAN_RADAR_PLUS.equals(previousPlan)) {
                currentPlan = CategoryCode.PLAN_RADAR;
            }
        } else {
            previousPlan = normalizeStorePlanCategory(previousPlan);
            currentPlan = normalizeStorePlanCategory(currentPlan);
            if (currentPlan == null && CategoryCode.PLAN_STORE_MONTHLY.equals(previousPlan)) {
                currentPlan = CategoryCode.SUSPENDED;
            }
        }

        applyCategoryTransition(planMetric, PLATFORM_SCOPE, previousPlan, currentPlan);
    }

    private CategoryCode normalizeBuyerPlanCategory(CategoryCode category) {
        if (category == null) {
            return null;
        }
        if (CategoryCode.SUSPENDED.equals(category)) {
            return CategoryCode.PLAN_RADAR;
        }
        return requireAllowedCategory(category, BUYER_CATEGORIES, MetricCode.PLATFORM_BUYERS_BY_PLAN);
    }

    private CategoryCode normalizeStorePlanCategory(CategoryCode category) {
        if (category == null) {
            return null;
        }
        return requireAllowedCategory(category, STORE_CATEGORIES, MetricCode.PLATFORM_STORES_BY_PLAN);
    }

    private CategoryCode requireAllowedCategory(
            CategoryCode category,
            Set<CategoryCode> allowed,
            MetricCode metricCode
    ) {
        if (category == null || !allowed.contains(category)) {
            throw new IllegalArgumentException(
                    "Unsupported category '" + (category != null ? category.value() : "null") + "' for " + metricCode);
        }
        return category;
    }

    private void applyCategoryTransition(
            MetricCode metricCode,
            Scope scope,
            CategoryCode previousCategory,
            CategoryCode newCategory
    ) {
        if (previousCategory != null && !previousCategory.equals(newCategory)) {
            metricRepository.adjustDistributionCount(metricCode, scope, previousCategory, -1L);
        }
        if (newCategory != null && !newCategory.equals(previousCategory)) {
            metricRepository.adjustDistributionCount(metricCode, scope, newCategory, 1L);
        }
    }

    private MetricCode resolvePlanMetric(DomainAnalyticsEvent event) {
        String subscriberType = event.subscriberType();
        if (subscriberType != null && !subscriberType.isBlank()) {
            String normalized = subscriberType.trim().toUpperCase(Locale.ROOT);
            if (normalized.startsWith("ROLE_")) {
                normalized = normalized.substring("ROLE_".length());
            }
            return switch (normalized) {
                case "BUYER", "COMPRADOR", "USUARIO", "USER", "PERSONA", "CLIENTE" ->
                        MetricCode.PLATFORM_BUYERS_BY_PLAN;
                case "STORE", "ALMACEN", "TIENDA", "STORE_ADMIN", "ADMIN_ALMACEN", "COMERCIO" ->
                        MetricCode.PLATFORM_STORES_BY_PLAN;
                default -> throw new IllegalArgumentException("Unsupported subscriberType: " + subscriberType);
            };
        }
        if ((event.categoryCode() != null && BUYER_CATEGORIES.contains(event.categoryCode()))
                || (event.previousCategoryCode() != null && BUYER_CATEGORIES.contains(event.previousCategoryCode()))) {
            return MetricCode.PLATFORM_BUYERS_BY_PLAN;
        }
        if ((event.categoryCode() != null && STORE_CATEGORIES.contains(event.categoryCode()))
                || (event.previousCategoryCode() != null && STORE_CATEGORIES.contains(event.previousCategoryCode()))) {
            return MetricCode.PLATFORM_STORES_BY_PLAN;
        }
        throw new IllegalArgumentException("Unable to determine subscription plan metric from event categories");
    }

    private Scope requireScope(String scopeId, ScopeType scopeType) {
        return new Scope(requireText(scopeId, "scopeId"), scopeType);
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
