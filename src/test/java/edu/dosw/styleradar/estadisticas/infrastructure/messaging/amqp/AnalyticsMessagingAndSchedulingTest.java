package edu.dosw.styleradar.estadisticas.infrastructure.messaging.amqp;

import edu.dosw.styleradar.estadisticas.domain.model.AnalyticsEvent;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.PaymentStatus;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.port.in.ConsolidateCountersUseCase;
import edu.dosw.styleradar.estadisticas.domain.port.in.RegisterAnalyticsEventUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.scheduling.CounterConsolidationScheduler;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AnalyticsMessagingAndSchedulingTest {

    private final EventToMetricMapper mapper = new EventToMetricMapper();

    @Test
    void mapsValidPayloadWithAndWithoutPaymentStatus() {
        Instant now = Instant.parse("2026-11-17T10:00:00Z");
        AnalyticsEventPayload storeVisit = new AnalyticsEventPayload(
                "evt-1",
                "STORE_PAGE_VISITS",
                "STORE",
                "store-17",
                new BigDecimal("1"),
                now,
                "   ");

        AnalyticsEvent mappedVisit = mapper.toAnalyticsEvent(storeVisit);
        assertEquals("evt-1", mappedVisit.eventId());
        assertEquals(MetricCode.STORE_PAGE_VISITS, mappedVisit.metricCode());
        assertEquals(ScopeType.STORE, mappedVisit.scope().type());
        assertEquals("store-17", mappedVisit.scope().id());
        assertNull(mappedVisit.paymentStatus());

        AnalyticsEventPayload revenue = new AnalyticsEventPayload(
                "evt-2",
                "PLATFORM_SUBSCRIPTION_REVENUE",
                "PLATFORM",
                "platform",
                new BigDecimal("29.99"),
                now,
                "APROBADO");

        AnalyticsEvent mappedRevenue = mapper.toAnalyticsEvent(revenue);
        assertEquals(PaymentStatus.APROBADO, mappedRevenue.paymentStatus());
    }

    @Test
    void rejectsNullOrInvalidPayloads() {
        Instant now = Instant.parse("2026-11-17T10:00:00Z");
        assertThrows(NullPointerException.class, () -> mapper.toAnalyticsEvent(null));

        assertThrows(IllegalArgumentException.class, () -> mapper.toAnalyticsEvent(
                new AnalyticsEventPayload("evt-1", "", "STORE", "store-17", BigDecimal.ONE, now, null)));

        assertThrows(IllegalArgumentException.class, () -> mapper.toAnalyticsEvent(
                new AnalyticsEventPayload("evt-1", "STORE_PAGE_VISITS", null, "store-17", BigDecimal.ONE, now, null)));

        assertThrows(IllegalArgumentException.class, () -> mapper.toAnalyticsEvent(
                new AnalyticsEventPayload("evt-1", "UNKNOWN_METRIC", "STORE", "store-17", BigDecimal.ONE, now, null)));
    }

    @Test
    void listenerDelegatesMappedEventToUseCase() {
        RegisterAnalyticsEventUseCase registerUseCase = mock(RegisterAnalyticsEventUseCase.class);
        AnalyticsEventListener listener = new AnalyticsEventListener(mapper, registerUseCase);
        Instant now = Instant.parse("2026-11-17T10:00:00Z");
        AnalyticsEventPayload payload = new AnalyticsEventPayload(
                "evt-3",
                "PLATFORM_SEARCHES",
                "PLATFORM",
                "platform",
                BigDecimal.ONE,
                now,
                null);

        listener.receive(payload);

        verify(registerUseCase).register(mapper.toAnalyticsEvent(payload));
    }

    @Test
    void schedulerInvokesConsolidationUseCase() {
        ConsolidateCountersUseCase consolidateUseCase = mock(ConsolidateCountersUseCase.class);
        CounterConsolidationScheduler scheduler = new CounterConsolidationScheduler(consolidateUseCase);

        scheduler.consolidateCounters();

        verify(consolidateUseCase).consolidate();
    }
}
