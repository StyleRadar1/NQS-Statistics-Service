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
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;

@Service
public class ProcessDomainEventUseCaseImpl implements ProcessDomainEventUseCase {

    private static final Duration EVENT_IDEMPOTENCY_TTL = Duration.ofHours(24);
    private static final Scope PLATFORM_SCOPE = new Scope("platform", ScopeType.PLATFORM);

    private final RealTimeCounterPort realTimeCounter;
    private final MetricRepositoryPort metricRepository;
    private final Clock clock;

    public ProcessDomainEventUseCaseImpl(
            RealTimeCounterPort realTimeCounter,
            MetricRepositoryPort metricRepository,
            Clock clock
    ) {
        this.realTimeCounter = realTimeCounter;
        this.metricRepository = metricRepository;
        this.clock = clock;
    }

    @Override
    public void process(DomainAnalyticsEvent event) {
        Objects.requireNonNull(event, "Domain analytics event must not be null");
        if (!realTimeCounter.markEventProcessed(event.eventId(), EVENT_IDEMPOTENCY_TTL)) {
            return;
        }

        LocalDate metricDate = event.occurredAt().atZone(clock.getZone()).toLocalDate();
        BigDecimal positiveAmount = (event.amount() != null && event.amount().signum() > 0)
                ? event.amount()
                : BigDecimal.ONE;
        long delta = event.delta() != 0L ? event.delta() : 1L;

        switch (event.eventType()) {
            case USER_SESSION_CHANGED -> {
                realTimeCounter.adjustActive(MetricCode.PLATFORM_REALTIME_USERS, PLATFORM_SCOPE, delta);
                realTimeCounter.adjustActive(MetricCode.PLATFORM_ACTIVE_USERS, PLATFORM_SCOPE, delta);
                if (hasText(event.scopeId())) {
                    realTimeCounter.adjustActive(
                            MetricCode.STORE_REALTIME_VISITORS,
                            new Scope(event.scopeId(), ScopeType.STORE),
                            delta);
                }
            }
            case FOUNDATION_STATUS_CHANGED ->
                    realTimeCounter.adjustActive(MetricCode.PLATFORM_ACTIVE_FOUNDATIONS, PLATFORM_SCOPE, delta);
            case STORE_VISIT_REGISTERED -> {
                Scope storeScope = requireScope(event.scopeId(), ScopeType.STORE);
                realTimeCounter.incrementDaily(MetricCode.STORE_PAGE_VISITS, storeScope, metricDate, positiveAmount);
                if (event.delta() != 0L) {
                    realTimeCounter.adjustActive(MetricCode.STORE_REALTIME_VISITORS, storeScope, event.delta());
                }
            }
            case STORE_VERIFICATION_CHANGED -> applyCategoryTransition(
                    MetricCode.PLATFORM_STORES_BY_VERIFICATION,
                    PLATFORM_SCOPE,
                    event.previousCategoryCode(),
                    event.categoryCode());
            case INVENTORY_STATE_CHANGED -> applyCategoryTransition(
                    MetricCode.STORE_INVENTORY_BY_STATE,
                    requireScope(event.scopeId(), ScopeType.STORE),
                    event.previousCategoryCode(),
                    event.categoryCode());
            case GARMENT_PUBLISHED ->
                    realTimeCounter.incrementDaily(
                            MetricCode.PLATFORM_GARMENT_PUBLICATIONS, PLATFORM_SCOPE, metricDate, positiveAmount);
            case GARMENT_MODERATION_PENDING -> {
                CategoryCode publisherType = requireCategory(
                        event.origin() != null ? event.origin() : event.categoryCode(),
                        "publisherType");
                String publisherId = requireText(event.itemId(), "itemId");
                metricRepository.upsertItemDailyIncrement(
                        MetricCode.MODERATION_PUBLICATIONS,
                        PLATFORM_SCOPE,
                        publisherType,
                        publisherId,
                        event.itemName(),
                        metricDate,
                        delta);
            }
            case CATALOG_SEARCH_EXECUTED ->
                    realTimeCounter.incrementDaily(
                            MetricCode.PLATFORM_SEARCHES, PLATFORM_SCOPE, metricDate, positiveAmount);
            case PLAYLIST_CREATED ->
                    realTimeCounter.incrementDaily(
                            MetricCode.PLATFORM_PLAYLISTS_CREATED, PLATFORM_SCOPE, metricDate, positiveAmount);
            case GARMENT_SAVED_IN_PLAYLIST -> {
                Scope storeScope = requireScope(event.scopeId(), ScopeType.STORE);
                String garmentId = requireText(event.itemId(), "itemId");
                metricRepository.upsertItemDailyIncrement(
                        MetricCode.STORE_GARMENTS_BY_SAVES,
                        storeScope,
                        null,
                        garmentId,
                        event.itemName(),
                        metricDate,
                        delta);
            }
            case STORE_MESSAGE_RECEIVED ->
                    realTimeCounter.incrementDaily(
                            MetricCode.STORE_MESSAGES_RECEIVED,
                            requireScope(event.scopeId(), ScopeType.STORE),
                            metricDate,
                            positiveAmount);
            case SUBSCRIPTION_PAYMENT_PROCESSED -> {
                if (event.paymentStatus() == PaymentStatus.APROBADO && event.amount() != null && event.amount().signum() > 0) {
                    realTimeCounter.incrementDaily(
                            MetricCode.PLATFORM_SUBSCRIPTION_REVENUE, PLATFORM_SCOPE, metricDate, event.amount());
                }
            }
            case SUBSCRIPTION_PLAN_CHANGED -> {
                MetricCode planMetric = resolvePlanMetric(event.categoryCode(), event.previousCategoryCode());
                applyCategoryTransition(planMetric, PLATFORM_SCOPE, event.previousCategoryCode(), event.categoryCode());
            }
            case DONATION_REQUEST_CREATED -> {
                Scope foundationScope = requireScope(event.scopeId(), ScopeType.FOUNDATION);
                CategoryCode origin = requireCategory(event.origin(), "origin");
                MetricCode requestMetric = CategoryCode.ORIGIN_STORE.equals(origin)
                        ? MetricCode.FOUNDATION_REQUESTS_FROM_STORES
                        : MetricCode.FOUNDATION_REQUESTS_FROM_USERS;
                realTimeCounter.incrementDaily(requestMetric, foundationScope, metricDate, positiveAmount);
                CategoryCode initialState = event.categoryCode() != null ? event.categoryCode() : CategoryCode.PENDING;
                metricRepository.adjustDistributionCount(
                        MetricCode.FOUNDATION_REQUESTS_BY_STATE, foundationScope, initialState, 1L);
            }
            case DONATION_REQUEST_STATE_CHANGED -> applyCategoryTransition(
                    MetricCode.FOUNDATION_REQUESTS_BY_STATE,
                    requireScope(event.scopeId(), ScopeType.FOUNDATION),
                    event.previousCategoryCode(),
                    event.categoryCode());
            case DONATION_COMPLETED -> {
                Scope foundationScope = requireScope(event.scopeId(), ScopeType.FOUNDATION);
                realTimeCounter.incrementDaily(
                        MetricCode.FOUNDATION_GARMENTS_RECEIVED, foundationScope, metricDate, positiveAmount);
                realTimeCounter.incrementDaily(
                        MetricCode.PLATFORM_MONTHLY_DONATIONS, PLATFORM_SCOPE, metricDate, BigDecimal.ONE);
                if (event.origin() != null) {
                    metricRepository.adjustDistributionCount(
                            MetricCode.FOUNDATION_DONATIONS_BY_ORIGIN, foundationScope, event.origin(), 1L);
                }
                if (event.categoryCode() != null) {
                    metricRepository.adjustDistributionCount(
                            MetricCode.FOUNDATION_GARMENTS_BY_TYPE,
                            foundationScope,
                            event.categoryCode(),
                            positiveAmount.longValue());
                }
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

    private MetricCode resolvePlanMetric(CategoryCode newPlan, CategoryCode previousPlan) {
        CategoryCode ref = newPlan != null ? newPlan : previousPlan;
        if (CategoryCode.PLAN_STORE_MONTHLY.equals(ref)) {
            return MetricCode.PLATFORM_STORES_BY_PLAN;
        }
        return MetricCode.PLATFORM_BUYERS_BY_PLAN;
    }

    private Scope requireScope(String scopeId, ScopeType scopeType) {
        return new Scope(requireText(scopeId, "scopeId"), scopeType);
    }

    private CategoryCode requireCategory(CategoryCode category, String field) {
        if (category == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return category;
    }

    private String requireText(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
