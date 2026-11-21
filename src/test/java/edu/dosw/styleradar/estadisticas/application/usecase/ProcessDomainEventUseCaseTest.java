package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.model.CategoryCode;
import edu.dosw.styleradar.estadisticas.domain.model.DomainAnalyticsEvent;
import edu.dosw.styleradar.estadisticas.domain.model.DomainEventType;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.PaymentStatus;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.port.out.MetricRepositoryPort;
import edu.dosw.styleradar.estadisticas.domain.port.out.RealTimeCounterPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProcessDomainEventUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-21T15:00:00Z"), ZoneOffset.UTC);
    private static final Instant OCCURRED_AT = Instant.parse("2026-11-21T14:30:00Z");
    private static final LocalDate DAY = LocalDate.of(2026, 11, 21);
    private static final Scope PLATFORM = new Scope("platform", ScopeType.PLATFORM);
    private static final Scope STORE = new Scope("store-17", ScopeType.STORE);
    private static final Scope FOUNDATION = new Scope("foundation-7", ScopeType.FOUNDATION);

    private RealTimeCounterPort realTimeCounter;
    private MetricRepositoryPort metricRepository;
    private ProcessDomainEventUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        realTimeCounter = mock(RealTimeCounterPort.class);
        metricRepository = mock(MetricRepositoryPort.class);
        when(realTimeCounter.markEventProcessed(any(), eq(Duration.ofHours(24)))).thenReturn(true);
        useCase = new ProcessDomainEventUseCaseImpl(realTimeCounter, metricRepository, CLOCK);
    }

    @Test
    void discardsDuplicateEventByRedisIdempotency() {
        when(realTimeCounter.markEventProcessed("evt-dup", Duration.ofHours(24))).thenReturn(false);

        useCase.process(new DomainAnalyticsEvent(
                "evt-dup", DomainEventType.CATALOG_SEARCH_EXECUTED, OCCURRED_AT,
                null, null, null, null, null, null, BigDecimal.ONE, 0L, null));

        verify(realTimeCounter, never()).incrementDaily(any(), any(), any(), any());
        verifyNoInteractions(metricRepository);
    }

    @Test
    void processesUserSessionAndFoundationStatusRealTimeEvents() {
        useCase.process(new DomainAnalyticsEvent(
                "evt-sess-1", DomainEventType.USER_SESSION_CHANGED, OCCURRED_AT,
                "store-17", null, null, null, null, null, null, 1L, null));
        verify(realTimeCounter).adjustActive(MetricCode.PLATFORM_REALTIME_USERS, PLATFORM, 1L);
        verify(realTimeCounter).adjustActive(MetricCode.PLATFORM_ACTIVE_USERS, PLATFORM, 1L);
        verify(realTimeCounter).adjustActive(MetricCode.STORE_REALTIME_VISITORS, STORE, 1L);

        useCase.process(new DomainAnalyticsEvent(
                "evt-fnd-1", DomainEventType.FOUNDATION_STATUS_CHANGED, OCCURRED_AT,
                null, null, null, null, null, null, null, -1L, null));
        verify(realTimeCounter).adjustActive(MetricCode.PLATFORM_ACTIVE_FOUNDATIONS, PLATFORM, -1L);
    }

    @Test
    void processesStoreVisitsVerificationInventoryAndMessages() {
        useCase.process(new DomainAnalyticsEvent(
                "evt-visit-1", DomainEventType.STORE_VISIT_REGISTERED, OCCURRED_AT,
                "store-17", null, null, null, null, null, BigDecimal.ONE, 1L, null));
        verify(realTimeCounter).incrementDaily(MetricCode.STORE_PAGE_VISITS, STORE, DAY, BigDecimal.ONE);
        verify(realTimeCounter).adjustActive(MetricCode.STORE_REALTIME_VISITORS, STORE, 1L);

        useCase.process(new DomainAnalyticsEvent(
                "evt-ver-1", DomainEventType.STORE_VERIFICATION_CHANGED, OCCURRED_AT,
                null, CategoryCode.APPROVED, CategoryCode.PENDING, null, null, null, null, 0L, null));
        verify(metricRepository).adjustDistributionCount(
                MetricCode.PLATFORM_STORES_BY_VERIFICATION, PLATFORM, CategoryCode.PENDING, -1L);
        verify(metricRepository).adjustDistributionCount(
                MetricCode.PLATFORM_STORES_BY_VERIFICATION, PLATFORM, CategoryCode.APPROVED, 1L);

        useCase.process(new DomainAnalyticsEvent(
                "evt-inv-1", DomainEventType.INVENTORY_STATE_CHANGED, OCCURRED_AT,
                "store-17", CategoryCode.LOW_STOCK, CategoryCode.AVAILABLE, null, null, null, null, 0L, null));
        verify(metricRepository).adjustDistributionCount(
                MetricCode.STORE_INVENTORY_BY_STATE, STORE, CategoryCode.AVAILABLE, -1L);
        verify(metricRepository).adjustDistributionCount(
                MetricCode.STORE_INVENTORY_BY_STATE, STORE, CategoryCode.LOW_STOCK, 1L);

        useCase.process(new DomainAnalyticsEvent(
                "evt-msg-1", DomainEventType.STORE_MESSAGE_RECEIVED, OCCURRED_AT,
                "store-17", null, null, null, null, null, BigDecimal.ONE, 0L, null));
        verify(realTimeCounter).incrementDaily(MetricCode.STORE_MESSAGES_RECEIVED, STORE, DAY, BigDecimal.ONE);
    }

    @Test
    void processesCatalogModerationSearchesAndPlaylistEvents() {
        useCase.process(new DomainAnalyticsEvent(
                "evt-gar-1", DomainEventType.GARMENT_PUBLISHED, OCCURRED_AT,
                null, null, null, null, null, null, BigDecimal.ONE, 0L, null));
        verify(realTimeCounter).incrementDaily(MetricCode.PLATFORM_GARMENT_PUBLICATIONS, PLATFORM, DAY, BigDecimal.ONE);

        useCase.process(new DomainAnalyticsEvent(
                "evt-mod-1", DomainEventType.GARMENT_MODERATION_PENDING, OCCURRED_AT,
                null, null, null, CategoryCode.ORIGIN_STORE, "store-17", "Tienda Aliada", null, 1L, null));
        verify(metricRepository).upsertItemDailyIncrement(
                MetricCode.MODERATION_PUBLICATIONS, PLATFORM, CategoryCode.ORIGIN_STORE,
                "store-17", "Tienda Aliada", DAY, 1L);

        useCase.process(new DomainAnalyticsEvent(
                "evt-search-1", DomainEventType.CATALOG_SEARCH_EXECUTED, OCCURRED_AT,
                null, null, null, null, null, null, BigDecimal.ONE, 0L, null));
        verify(realTimeCounter).incrementDaily(MetricCode.PLATFORM_SEARCHES, PLATFORM, DAY, BigDecimal.ONE);

        useCase.process(new DomainAnalyticsEvent(
                "evt-pl-1", DomainEventType.PLAYLIST_CREATED, OCCURRED_AT,
                null, null, null, null, null, null, BigDecimal.ONE, 0L, null));
        verify(realTimeCounter).incrementDaily(MetricCode.PLATFORM_PLAYLISTS_CREATED, PLATFORM, DAY, BigDecimal.ONE);

        useCase.process(new DomainAnalyticsEvent(
                "evt-save-1", DomainEventType.GARMENT_SAVED_IN_PLAYLIST, OCCURRED_AT,
                "store-17", null, null, null, "garment-1", "Chaqueta Denim", null, 1L, null));
        verify(metricRepository).upsertItemDailyIncrement(
                MetricCode.STORE_GARMENTS_BY_SAVES, STORE, null, "garment-1", "Chaqueta Denim", DAY, 1L);
    }

    @Test
    void processesSubscriptionPaymentsOnlyWhenApprovedAndTracksBuyerAndStorePlans() {
        useCase.process(new DomainAnalyticsEvent(
                "evt-pay-ok", DomainEventType.SUBSCRIPTION_PAYMENT_PROCESSED, OCCURRED_AT,
                null, null, null, null, null, null, new BigDecimal("89000"), 0L, PaymentStatus.APROBADO));
        verify(realTimeCounter).incrementDaily(
                MetricCode.PLATFORM_SUBSCRIPTION_REVENUE, PLATFORM, DAY, new BigDecimal("89000"));

        useCase.process(new DomainAnalyticsEvent(
                "evt-pay-ko", DomainEventType.SUBSCRIPTION_PAYMENT_PROCESSED, OCCURRED_AT,
                null, null, null, null, null, null, new BigDecimal("89000"), 0L, PaymentStatus.RECHAZADO));
        verify(realTimeCounter, never()).incrementDaily(
                eq(MetricCode.PLATFORM_SUBSCRIPTION_REVENUE), eq(PLATFORM), eq(DAY), eq(BigDecimal.ONE));

        useCase.process(new DomainAnalyticsEvent(
                "evt-plan-buyer", DomainEventType.SUBSCRIPTION_PLAN_CHANGED, OCCURRED_AT,
                null, CategoryCode.PLAN_RADAR_PLUS, CategoryCode.PLAN_RADAR, null, null, null, null, 0L, null));
        verify(metricRepository).adjustDistributionCount(
                MetricCode.PLATFORM_BUYERS_BY_PLAN, PLATFORM, CategoryCode.PLAN_RADAR, -1L);
        verify(metricRepository).adjustDistributionCount(
                MetricCode.PLATFORM_BUYERS_BY_PLAN, PLATFORM, CategoryCode.PLAN_RADAR_PLUS, 1L);

        useCase.process(new DomainAnalyticsEvent(
                "evt-plan-store", DomainEventType.SUBSCRIPTION_PLAN_CHANGED, OCCURRED_AT,
                null, CategoryCode.PLAN_STORE_MONTHLY, null, null, null, null, null, 0L, null));
        verify(metricRepository).adjustDistributionCount(
                MetricCode.PLATFORM_STORES_BY_PLAN, PLATFORM, CategoryCode.PLAN_STORE_MONTHLY, 1L);
    }

    @Test
    void processesDonationRequestsStateChangesAndCompletedDonations() {
        useCase.process(new DomainAnalyticsEvent(
                "evt-don-req-user", DomainEventType.DONATION_REQUEST_CREATED, OCCURRED_AT,
                "foundation-7", null, null, CategoryCode.ORIGIN_USER, null, null, BigDecimal.ONE, 0L, null));
        verify(realTimeCounter).incrementDaily(
                MetricCode.FOUNDATION_REQUESTS_FROM_USERS, FOUNDATION, DAY, BigDecimal.ONE);
        verify(metricRepository).adjustDistributionCount(
                MetricCode.FOUNDATION_REQUESTS_BY_STATE, FOUNDATION, CategoryCode.PENDING, 1L);

        useCase.process(new DomainAnalyticsEvent(
                "evt-don-req-store", DomainEventType.DONATION_REQUEST_CREATED, OCCURRED_AT,
                "foundation-7", CategoryCode.PENDING, null, CategoryCode.ORIGIN_STORE, null, null, BigDecimal.ONE, 0L, null));
        verify(realTimeCounter).incrementDaily(
                MetricCode.FOUNDATION_REQUESTS_FROM_STORES, FOUNDATION, DAY, BigDecimal.ONE);

        useCase.process(new DomainAnalyticsEvent(
                "evt-don-state", DomainEventType.DONATION_REQUEST_STATE_CHANGED, OCCURRED_AT,
                "foundation-7", CategoryCode.APPROVED, CategoryCode.PENDING, null, null, null, null, 0L, null));
        verify(metricRepository).adjustDistributionCount(
                MetricCode.FOUNDATION_REQUESTS_BY_STATE, FOUNDATION, CategoryCode.PENDING, -1L);
        verify(metricRepository).adjustDistributionCount(
                MetricCode.FOUNDATION_REQUESTS_BY_STATE, FOUNDATION, CategoryCode.APPROVED, 1L);

        useCase.process(new DomainAnalyticsEvent(
                "evt-don-done", DomainEventType.DONATION_COMPLETED, OCCURRED_AT,
                "foundation-7", new CategoryCode("JACKET"), null, CategoryCode.ORIGIN_STORE,
                null, null, BigDecimal.valueOf(5), 0L, null));
        verify(realTimeCounter).incrementDaily(
                MetricCode.FOUNDATION_GARMENTS_RECEIVED, FOUNDATION, DAY, BigDecimal.valueOf(5));
        verify(realTimeCounter).incrementDaily(
                MetricCode.PLATFORM_MONTHLY_DONATIONS, PLATFORM, DAY, BigDecimal.ONE);
        verify(metricRepository).adjustDistributionCount(
                MetricCode.FOUNDATION_DONATIONS_BY_ORIGIN, FOUNDATION, CategoryCode.ORIGIN_STORE, 1L);
        verify(metricRepository).adjustDistributionCount(
                MetricCode.FOUNDATION_GARMENTS_BY_TYPE, FOUNDATION, new CategoryCode("JACKET"), 5L);
    }

    @Test
    void rejectsMissingRequiredFieldsForScopedEvents() {
        assertThrows(IllegalArgumentException.class, () -> useCase.process(new DomainAnalyticsEvent(
                "evt-bad-1", DomainEventType.STORE_VISIT_REGISTERED, OCCURRED_AT,
                " ", null, null, null, null, null, BigDecimal.ONE, 0L, null)));
        assertThrows(IllegalArgumentException.class, () -> useCase.process(new DomainAnalyticsEvent(
                "evt-bad-2", DomainEventType.GARMENT_MODERATION_PENDING, OCCURRED_AT,
                null, null, null, null, "store-1", "Store", null, 1L, null)));
        assertThrows(IllegalArgumentException.class, () -> useCase.process(new DomainAnalyticsEvent(
                "evt-bad-3", DomainEventType.GARMENT_SAVED_IN_PLAYLIST, OCCURRED_AT,
                "store-1", null, null, null, " ", "Prenda", null, 1L, null)));
    }
}
