package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.model.CategoryCode;
import edu.dosw.styleradar.estadisticas.domain.model.DomainAnalyticsEvent;
import edu.dosw.styleradar.estadisticas.domain.model.DomainEventType;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.PaymentStatus;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.port.out.MetricRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ProcessDomainEventUseCaseTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-11-21T14:30:00Z");
    private static final LocalDate DAY = LocalDate.of(2026, 11, 21);
    private static final Scope PLATFORM = new Scope("platform", ScopeType.PLATFORM);
    private static final Scope FOUNDATION = new Scope("foundation-7", ScopeType.FOUNDATION);

    private MetricRepositoryPort metricRepository;
    private ProcessDomainEventUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        metricRepository = mock(MetricRepositoryPort.class);
        useCase = new ProcessDomainEventUseCaseImpl(metricRepository);
    }

    @Test
    void processesUserRegistrationsAndInitialBuyerPlan() {
        useCase.process(new DomainAnalyticsEvent(
                "evt-usr-buyer", DomainEventType.USER_REGISTERED, Instant.parse("2026-11-01T02:00:00Z"),
                null, CategoryCode.PLAN_RADAR, null, BigDecimal.ONE, 0L, null));
        verify(metricRepository).upsertDailyIncrement(
                MetricCode.PLATFORM_USER_REGISTRATIONS, PLATFORM, LocalDate.of(2026, 10, 31), BigDecimal.ONE);
        verify(metricRepository).adjustDistributionCount(
                MetricCode.PLATFORM_BUYERS_BY_PLAN, PLATFORM, CategoryCode.PLAN_RADAR, 1L);

        useCase.process(new DomainAnalyticsEvent(
                "evt-usr-store", DomainEventType.USER_REGISTERED, OCCURRED_AT,
                null, null, null, BigDecimal.ONE, 0L, null));
        verify(metricRepository).upsertDailyIncrement(
                MetricCode.PLATFORM_USER_REGISTRATIONS, PLATFORM, DAY, BigDecimal.ONE);
    }

    @Test
    void processesInventoryTransitionsUnderPlatformScope() {
        useCase.process(new DomainAnalyticsEvent(
                "evt-inv-noop", DomainEventType.INVENTORY_STATE_CHANGED, OCCURRED_AT,
                "store-17", CategoryCode.LOW_STOCK, CategoryCode.LOW_STOCK, null, 0L, null));
        verifyNoInteractions(metricRepository);

        useCase.process(new DomainAnalyticsEvent(
                "evt-inv-create", DomainEventType.INVENTORY_STATE_CHANGED, OCCURRED_AT,
                "store-17", CategoryCode.AVAILABLE, null, null, 0L, null));
        verify(metricRepository).adjustDistributionCount(
                MetricCode.STORE_INVENTORY_BY_STATE, PLATFORM, CategoryCode.AVAILABLE, 1L);

        useCase.process(new DomainAnalyticsEvent(
                "evt-inv-move", DomainEventType.INVENTORY_STATE_CHANGED, OCCURRED_AT,
                "store-17", CategoryCode.LOW_STOCK, CategoryCode.AVAILABLE, null, 0L, null));
        verify(metricRepository).adjustDistributionCount(
                MetricCode.STORE_INVENTORY_BY_STATE, PLATFORM, CategoryCode.AVAILABLE, -1L);
        verify(metricRepository).adjustDistributionCount(
                MetricCode.STORE_INVENTORY_BY_STATE, PLATFORM, CategoryCode.LOW_STOCK, 1L);
    }

    @Test
    void processesGarmentPublicationsPlaylistsAndDonations() {
        useCase.process(new DomainAnalyticsEvent(
                "evt-gar-1", DomainEventType.GARMENT_PUBLISHED, OCCURRED_AT,
                null, null, null, BigDecimal.ONE, 0L, null));
        verify(metricRepository).upsertDailyIncrement(MetricCode.PLATFORM_GARMENT_PUBLICATIONS, PLATFORM, DAY, BigDecimal.ONE);

        useCase.process(new DomainAnalyticsEvent(
                "evt-pl-1", DomainEventType.PLAYLIST_CREATED, OCCURRED_AT,
                null, null, null, null, 0L, null));
        verify(metricRepository).upsertDailyIncrement(MetricCode.PLATFORM_PLAYLISTS_CREATED, PLATFORM, DAY, BigDecimal.ONE);

        useCase.process(new DomainAnalyticsEvent(
                "evt-don-done", DomainEventType.DONATION_COMPLETED, OCCURRED_AT,
                "foundation-7", null, null, BigDecimal.valueOf(5), 0L, null));
        verify(metricRepository).upsertDailyIncrement(
                MetricCode.FOUNDATION_GARMENTS_RECEIVED, FOUNDATION, DAY, BigDecimal.valueOf(5));
        verify(metricRepository).upsertDailyIncrement(
                MetricCode.PLATFORM_MONTHLY_DONATIONS, PLATFORM, DAY, BigDecimal.ONE);
    }

    @Test
    void processesSubscriptionPaymentsAndIgnoresMissingOrRejectedAmounts() {
        useCase.process(new DomainAnalyticsEvent(
                "evt-pay-ok", DomainEventType.SUBSCRIPTION_PAYMENT_PROCESSED, OCCURRED_AT,
                null, null, null, new BigDecimal("89000"), 0L, PaymentStatus.APROBADO));
        verify(metricRepository).upsertDailyIncrement(
                MetricCode.PLATFORM_SUBSCRIPTION_REVENUE, PLATFORM, DAY, new BigDecimal("89000"));

        clearInvocations(metricRepository);
        useCase.process(new DomainAnalyticsEvent(
                "evt-pay-no-amount", DomainEventType.SUBSCRIPTION_PAYMENT_PROCESSED, OCCURRED_AT,
                null, null, null, null, 0L, PaymentStatus.APROBADO));
        useCase.process(new DomainAnalyticsEvent(
                "evt-pay-zero", DomainEventType.SUBSCRIPTION_PAYMENT_PROCESSED, OCCURRED_AT,
                null, null, null, BigDecimal.ZERO, 0L, PaymentStatus.APROBADO));
        useCase.process(new DomainAnalyticsEvent(
                "evt-pay-ko", DomainEventType.SUBSCRIPTION_PAYMENT_PROCESSED, OCCURRED_AT,
                null, null, null, new BigDecimal("89000"), 0L, PaymentStatus.RECHAZADO));
        verify(metricRepository, never()).upsertDailyIncrement(any(), any(), any(), any());
    }

    @Test
    void processesBuyerAndStorePlanTransitionsSafely() {
        useCase.process(new DomainAnalyticsEvent(
                "evt-plan-buyer", DomainEventType.SUBSCRIPTION_PLAN_CHANGED, OCCURRED_AT,
                null, CategoryCode.PLAN_RADAR_PLUS, CategoryCode.PLAN_RADAR, null, 0L, null, "USUARIO"));
        verify(metricRepository).adjustDistributionCount(
                MetricCode.PLATFORM_BUYERS_BY_PLAN, PLATFORM, CategoryCode.PLAN_RADAR, -1L);
        verify(metricRepository).adjustDistributionCount(
                MetricCode.PLATFORM_BUYERS_BY_PLAN, PLATFORM, CategoryCode.PLAN_RADAR_PLUS, 1L);

        clearInvocations(metricRepository);
        useCase.process(new DomainAnalyticsEvent(
                "evt-plan-buyer-suspended", DomainEventType.SUBSCRIPTION_PLAN_CHANGED, OCCURRED_AT,
                null, CategoryCode.SUSPENDED, CategoryCode.PLAN_RADAR_PLUS, null, 0L, null, "BUYER"));
        verify(metricRepository).adjustDistributionCount(
                MetricCode.PLATFORM_BUYERS_BY_PLAN, PLATFORM, CategoryCode.PLAN_RADAR_PLUS, -1L);
        verify(metricRepository).adjustDistributionCount(
                MetricCode.PLATFORM_BUYERS_BY_PLAN, PLATFORM, CategoryCode.PLAN_RADAR, 1L);
        verify(metricRepository, never()).adjustDistributionCount(
                MetricCode.PLATFORM_BUYERS_BY_PLAN, PLATFORM, CategoryCode.SUSPENDED, 1L);

        clearInvocations(metricRepository);
        useCase.process(new DomainAnalyticsEvent(
                "evt-plan-buyer-null-downgrade", DomainEventType.SUBSCRIPTION_PLAN_CHANGED, OCCURRED_AT,
                null, null, CategoryCode.PLAN_RADAR_PLUS, null, 0L, null));
        verify(metricRepository).adjustDistributionCount(
                MetricCode.PLATFORM_BUYERS_BY_PLAN, PLATFORM, CategoryCode.PLAN_RADAR_PLUS, -1L);
        verify(metricRepository).adjustDistributionCount(
                MetricCode.PLATFORM_BUYERS_BY_PLAN, PLATFORM, CategoryCode.PLAN_RADAR, 1L);

        clearInvocations(metricRepository);
        useCase.process(new DomainAnalyticsEvent(
                "evt-plan-store-init", DomainEventType.SUBSCRIPTION_PLAN_CHANGED, OCCURRED_AT,
                null, CategoryCode.PLAN_STORE_MONTHLY, null, null, 0L, null, "ALMACEN"));
        verify(metricRepository).adjustDistributionCount(
                MetricCode.PLATFORM_STORES_BY_PLAN, PLATFORM, CategoryCode.PLAN_STORE_MONTHLY, 1L);

        clearInvocations(metricRepository);
        useCase.process(new DomainAnalyticsEvent(
                "evt-plan-store-null-suspend", DomainEventType.SUBSCRIPTION_PLAN_CHANGED, OCCURRED_AT,
                null, null, CategoryCode.PLAN_STORE_MONTHLY, null, 0L, null, "STORE"));
        verify(metricRepository).adjustDistributionCount(
                MetricCode.PLATFORM_STORES_BY_PLAN, PLATFORM, CategoryCode.PLAN_STORE_MONTHLY, -1L);
        verify(metricRepository).adjustDistributionCount(
                MetricCode.PLATFORM_STORES_BY_PLAN, PLATFORM, CategoryCode.SUSPENDED, 1L);
    }

    @Test
    void rejectsMissingScopeAndIncompatibleCategories() {
        assertThrows(IllegalArgumentException.class, () -> useCase.process(new DomainAnalyticsEvent(
                "evt-bad-inv-cat", DomainEventType.INVENTORY_STATE_CHANGED, OCCURRED_AT,
                "store-17", CategoryCode.PLAN_RADAR, null, null, 0L, null)));
        assertThrows(IllegalArgumentException.class, () -> useCase.process(new DomainAnalyticsEvent(
                "evt-bad-don", DomainEventType.DONATION_COMPLETED, OCCURRED_AT,
                " ", null, null, BigDecimal.ONE, 0L, null)));
        assertThrows(IllegalArgumentException.class, () -> useCase.process(new DomainAnalyticsEvent(
                "evt-bad-plan-empty", DomainEventType.SUBSCRIPTION_PLAN_CHANGED, OCCURRED_AT,
                null, null, null, null, 0L, null, "BUYER")));
        assertThrows(IllegalArgumentException.class, () -> useCase.process(new DomainAnalyticsEvent(
                "evt-bad-plan-mismatch", DomainEventType.SUBSCRIPTION_PLAN_CHANGED, OCCURRED_AT,
                null, CategoryCode.PLAN_STORE_MONTHLY, null, null, 0L, null, "BUYER")));
        assertThrows(IllegalArgumentException.class, () -> useCase.process(new DomainAnalyticsEvent(
                "evt-bad-plan-store-mismatch", DomainEventType.SUBSCRIPTION_PLAN_CHANGED, OCCURRED_AT,
                null, CategoryCode.PLAN_RADAR, null, null, 0L, null, "STORE")));
        assertThrows(IllegalArgumentException.class, () -> useCase.process(new DomainAnalyticsEvent(
                "evt-bad-subscriber", DomainEventType.SUBSCRIPTION_PLAN_CHANGED, OCCURRED_AT,
                null, CategoryCode.PLAN_RADAR, null, null, 0L, null, "UNKNOWN_TYPE")));
    }
}
