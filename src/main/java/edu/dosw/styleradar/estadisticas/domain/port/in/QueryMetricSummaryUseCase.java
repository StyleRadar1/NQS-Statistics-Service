package edu.dosw.styleradar.estadisticas.domain.port.in;

import edu.dosw.styleradar.estadisticas.domain.model.ComparisonWindow;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.MetricSummaryWithVariation;
import edu.dosw.styleradar.estadisticas.domain.model.MetricTotal;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;

public interface QueryMetricSummaryUseCase {
    MetricSummaryWithVariation queryWithVariation(MetricCode metricCode, Scope scope, ComparisonWindow window);

    default MetricTotal queryTotal(MetricCode metricCode, Scope scope) {
        throw new UnsupportedOperationException("Metric totals are not supported");
    }
}
