package edu.dosw.styleradar.estadisticas.domain.port.in;

import edu.dosw.styleradar.estadisticas.domain.model.DomainAnalyticsEvent;

public interface ProcessDomainEventUseCase {

    void process(DomainAnalyticsEvent event);
}
