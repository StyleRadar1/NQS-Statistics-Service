package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.model.CategoryCode;
import edu.dosw.styleradar.estadisticas.domain.model.DomainAnalyticsEvent;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.PaymentStatus;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.port.in.ProcessDomainEventUseCase;
import edu.dosw.styleradar.estadisticas.domain.port.out.MetricRepositoryPort;
import edu.dosw.styleradar.estadisticas.domain.port.out.RealTimeCounterPort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;

@Service
public class ProcessDomainEventUseCaseImpl implements ProcessDomainEventUseCase {

    private static final Duration EVENT_IDEMPOTENCY_TTL = Duration.ofHours(24);
    private static final Scope PLATFORM_SCOPE = new Scope("platform", ScopeType.PLATFORM);
    private static final ZoneId BOGOTA_ZONE = ZoneId.of("America/Bogota");

    private final RealTimeCounterPort realTimeCounter;
    private final MetricRepositoryPort metricRepository;

    public ProcessDomainEventUseCaseImpl(
            RealTimeCounterPort realTimeCounter,
            MetricRepositoryPort metricRepository
    ) {
        this.realTimeCounter = realTimeCounter;
        this.metricRepository = metricRepository;
    }

    @Override
    public void process(DomainAnalyticsEvent event) {
        Objects.requireNonNull(event, "Domain analytics event must not be null");
        if (!realTimeCounter.markEventProcessed(event.eventId(), EVENT_IDEMPOTENCY_TTL)) {
            return;
        }

        LocalDate metricDate = event.occurredAt().atZone(BOGOTA_ZONE).toLocalDate();
        BigDecimal positiveAmount = (event.amount() != null && event.amount().signum() > 0)
                ? event.amount()
                : BigDecimal.ONE;

        switch (event.eventType()) {
            case USER_REGISTERED -> {
                realTimeCounter.incrementDaily(
                        MetricCode.PLATFORM_USER_REGISTRATIONS, PLATFORM_SCOPE, metricDate, BigDecimal.ONE);
                if (CategoryCode.PLAN_RADAR.equals(event.categoryCode())) {
                    metricRepository.adjustDistributionCount(
                            MetricCode.PLATFORM_BUYERS_BY_PLAN, PLATFORM_SCOPE, CategoryCode.PLAN_RADAR, 1L);
                }
            }
            case INVENTORY_STATE_CHANGED -> applyCategoryTransition(
                    MetricCode.STORE_INVENTORY_BY_STATE,
                    requireScope(event.scopeId(), ScopeType.STORE),
                    event.previousCategoryCode(),
                    event.categoryCode());
            case GARMENT_PUBLISHED -> realTimeCounter.incrementDaily(
                    MetricCode.PLATFORM_GARMENT_PUBLICATIONS, PLATFORM_SCOPE, metricDate, positiveAmount);
            case PLAYLIST_CREATED -> realTimeCounter.incrementDaily(
                    MetricCode.PLATFORM_PLAYLISTS_CREATED, PLATFORM_SCOPE, metricDate, positiveAmount);
            case SUBSCRIPTION_PAYMENT_PROCESSED -> {
                if (event.paymentStatus() == PaymentStatus.APROBADO
                        && event.amount() != null
                        && event.amount().signum() > 0) {
                    realTimeCounter.incrementDaily(
                            MetricCode.PLATFORM_SUBSCRIPTION_REVENUE, PLATFORM_SCOPE, metricDate, event.amount());
                }
            }
            case SUBSCRIPTION_PLAN_CHANGED -> {
                MetricCode planMetric = resolvePlanMetric(event);
                applyCategoryTransition(planMetric, PLATFORM_SCOPE, event.previousCategoryCode(), event.categoryCode());
            }
            case DONATION_COMPLETED -> {
                Scope foundationScope = requireScope(event.scopeId(), ScopeType.FOUNDATION);
                realTimeCounter.incrementDaily(
                        MetricCode.FOUNDATION_GARMENTS_RECEIVED, foundationScope, metricDate, positiveAmount);
                realTimeCounter.incrementDaily(
                        MetricCode.PLATFORM_MONTHLY_DONATIONS, PLATFORM_SCOPE, metricDate, BigDecimal.ONE);
            }
        }
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
            if ("BUYER".equals(normalized) || "COMPRADOR".equals(normalized) || "USER".equals(normalized)) {
                return MetricCode.PLATFORM_BUYERS_BY_PLAN;
            }
            if ("STORE".equals(normalized) || "ALMACEN".equals(normalized) || "TIENDA".equals(normalized)) {
                return MetricCode.PLATFORM_STORES_BY_PLAN;
            }
        }
        CategoryCode ref = event.categoryCode() != null ? event.categoryCode() : event.previousCategoryCode();
        if (CategoryCode.PLAN_STORE_MONTHLY.equals(ref) || CategoryCode.SUSPENDED.equals(ref)) {
            return MetricCode.PLATFORM_STORES_BY_PLAN;
        }
        return MetricCode.PLATFORM_BUYERS_BY_PLAN;
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
