package edu.dosw.styleradar.estadisticas.domain.port.in;

import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Period;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.TimeGranularity;
import edu.dosw.styleradar.estadisticas.domain.model.TimeSeries;

public interface QueryTimeSeriesUseCase {
    TimeSeries query(MetricCode metricCode, Scope scope, Period requestedPeriod, TimeGranularity granularity);
}
