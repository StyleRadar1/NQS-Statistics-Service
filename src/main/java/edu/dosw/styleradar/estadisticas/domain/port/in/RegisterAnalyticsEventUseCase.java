package edu.dosw.styleradar.estadisticas.domain.port.in;

import edu.dosw.styleradar.estadisticas.domain.model.AnalyticsEvent;

public interface RegisterAnalyticsEventUseCase {
    void register(AnalyticsEvent event);
}
