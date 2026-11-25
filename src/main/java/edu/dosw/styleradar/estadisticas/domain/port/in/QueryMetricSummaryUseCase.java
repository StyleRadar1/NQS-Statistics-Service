package edu.dosw.styleradar.estadisticas.domain.port.in;

import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.MetricTotal;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;

public interface QueryMetricSummaryUseCase {
    MetricTotal queryTotal(MetricCode metricCode, Scope scope);
}
