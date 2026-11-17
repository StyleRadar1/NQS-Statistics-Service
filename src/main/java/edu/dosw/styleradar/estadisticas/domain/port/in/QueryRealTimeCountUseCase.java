package edu.dosw.styleradar.estadisticas.domain.port.in;

import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.RealTimeCount;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;

public interface QueryRealTimeCountUseCase {
    RealTimeCount query(MetricCode metricCode, Scope scope);
}
