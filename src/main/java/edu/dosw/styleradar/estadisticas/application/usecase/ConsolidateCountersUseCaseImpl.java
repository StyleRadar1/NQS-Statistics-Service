package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.model.DailyMetricIncrement;
import edu.dosw.styleradar.estadisticas.domain.port.in.ConsolidateCountersUseCase;
import edu.dosw.styleradar.estadisticas.domain.port.out.MetricRepositoryPort;
import edu.dosw.styleradar.estadisticas.domain.port.out.RealTimeCounterPort;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ConsolidateCountersUseCaseImpl implements ConsolidateCountersUseCase {

    private final RealTimeCounterPort realTimeCounter;
    private final MetricRepositoryPort metricRepository;

    public ConsolidateCountersUseCaseImpl(
            RealTimeCounterPort realTimeCounter,
            MetricRepositoryPort metricRepository
    ) {
        this.realTimeCounter = realTimeCounter;
        this.metricRepository = metricRepository;
    }

    @Override
    public void consolidate() {
        for (DailyMetricIncrement increment : realTimeCounter.drainDailyCounters()) {
            Objects.requireNonNull(increment, "Drained daily increment must not be null");
            metricRepository.upsertDailyIncrement(
                    increment.metricCode(),
                    increment.scope(),
                    increment.metricDate(),
                    increment.value());
        }
    }
}
