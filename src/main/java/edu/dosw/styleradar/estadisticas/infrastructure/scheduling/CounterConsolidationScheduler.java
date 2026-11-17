package edu.dosw.styleradar.estadisticas.infrastructure.scheduling;

import edu.dosw.styleradar.estadisticas.domain.port.in.ConsolidateCountersUseCase;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class CounterConsolidationScheduler {

    private final ConsolidateCountersUseCase consolidateCountersUseCase;

    public CounterConsolidationScheduler(ConsolidateCountersUseCase consolidateCountersUseCase) {
        this.consolidateCountersUseCase = consolidateCountersUseCase;
    }

    @Scheduled(
            fixedDelayString = "${statistics.consolidation.fixed-delay:60000}",
            initialDelayString = "${statistics.consolidation.initial-delay:60000}")
    public void consolidateCounters() {
        consolidateCountersUseCase.consolidate();
    }
}
