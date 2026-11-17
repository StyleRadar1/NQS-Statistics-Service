package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.model.DailyMetricIncrement;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.port.out.MetricRepositoryPort;
import edu.dosw.styleradar.estadisticas.domain.port.out.RealTimeCounterPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsolidateCountersUseCaseTest {

    @Mock
    private RealTimeCounterPort realTimeCounter;

    @Mock
    private MetricRepositoryPort metricRepository;

    @Test
    void drainsDailyCountersAndPersistsEachIncrementExactlyOnce() {
        DailyMetricIncrement registrations = new DailyMetricIncrement(
                MetricCode.PLATFORM_USER_REGISTRATIONS,
                new Scope("platform", ScopeType.PLATFORM),
                LocalDate.of(2026, 11, 17),
                new BigDecimal("3"));
        DailyMetricIncrement revenue = new DailyMetricIncrement(
                MetricCode.PLATFORM_SUBSCRIPTION_REVENUE,
                new Scope("platform", ScopeType.PLATFORM),
                LocalDate.of(2026, 11, 17),
                new BigDecimal("19.99"));
        when(realTimeCounter.drainDailyCounters()).thenReturn(List.of(registrations, revenue));
        ConsolidateCountersUseCaseImpl useCase =
                new ConsolidateCountersUseCaseImpl(realTimeCounter, metricRepository);

        useCase.consolidate();

        verify(realTimeCounter).drainDailyCounters();
        verify(metricRepository).upsertDailyIncrement(
                registrations.metricCode(), registrations.scope(), registrations.metricDate(), registrations.value());
        verify(metricRepository).upsertDailyIncrement(
                revenue.metricCode(), revenue.scope(), revenue.metricDate(), revenue.value());
        verifyNoMoreInteractions(metricRepository);
    }
}
