package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.model.AnalyticsEvent;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.PaymentStatus;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.port.out.RealTimeCounterPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterAnalyticsEventUseCaseTest {

    private static final Scope PLATFORM_SCOPE = new Scope("platform", ScopeType.PLATFORM);
    private static final Instant OCCURRED_AT = Instant.parse("2026-11-17T14:00:00Z");

    @Mock
    private RealTimeCounterPort realTimeCounter;

    private RegisterAnalyticsEventUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterAnalyticsEventUseCaseImpl(
                realTimeCounter,
                Clock.fixed(OCCURRED_AT, ZoneOffset.UTC));
    }

    @Test
    void ignoresDuplicateEventBeforeIncrementingCounters() {
        AnalyticsEvent event = event(
                "duplicate-event",
                MetricCode.PLATFORM_USER_REGISTRATIONS,
                new BigDecimal("1"),
                null);
        when(realTimeCounter.markEventProcessed("duplicate-event", Duration.ofHours(24)))
                .thenReturn(false);

        useCase.register(event);

        verify(realTimeCounter).markEventProcessed("duplicate-event", Duration.ofHours(24));
        verify(realTimeCounter, never()).incrementDaily(
                eq(event.metricCode()), eq(event.scope()), eq(LocalDate.of(2026, 11, 17)), eq(event.value()));
        verifyNoInteractionsAfterIdempotencyCheck();
        verifyNoMoreInteractions(realTimeCounter);
    }

    @Test
    void onlyApprovedSubscriptionPaymentsIncrementRevenue() {
        when(realTimeCounter.markEventProcessed(
                org.mockito.ArgumentMatchers.anyString(),
                eq(Duration.ofHours(24))))
                .thenReturn(true);

        for (PaymentStatus status : PaymentStatus.values()) {
            useCase.register(event(
                    "payment-" + status.name(),
                    MetricCode.PLATFORM_SUBSCRIPTION_REVENUE,
                    new BigDecimal("19.99"),
                    status));
        }

        verify(realTimeCounter, times(1)).incrementDaily(
                eq(MetricCode.PLATFORM_SUBSCRIPTION_REVENUE),
                eq(PLATFORM_SCOPE),
                eq(LocalDate.of(2026, 11, 17)),
                eq(new BigDecimal("19.99")));
        verify(realTimeCounter, never()).incrementActive(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void incrementsRealtimeAndDailyCountersForRealtimeMetrics() {
        AnalyticsEvent event = event(
                "active-user",
                MetricCode.PLATFORM_ACTIVE_USERS,
                new BigDecimal("2"),
                null);
        when(realTimeCounter.markEventProcessed("active-user", Duration.ofHours(24)))
                .thenReturn(true);

        useCase.register(event);

        verify(realTimeCounter).incrementActive(MetricCode.PLATFORM_ACTIVE_USERS, PLATFORM_SCOPE, 2);
        verify(realTimeCounter).incrementDaily(
                MetricCode.PLATFORM_ACTIVE_USERS,
                PLATFORM_SCOPE,
                LocalDate.of(2026, 11, 17),
                new BigDecimal("2"));
    }

    private AnalyticsEvent event(
            String id,
            MetricCode metricCode,
            BigDecimal value,
            PaymentStatus paymentStatus
    ) {
        return new AnalyticsEvent(id, metricCode, PLATFORM_SCOPE, value, OCCURRED_AT, paymentStatus);
    }

    private void verifyNoInteractionsAfterIdempotencyCheck() {
        verify(realTimeCounter, never()).incrementActive(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyLong());
    }
}
