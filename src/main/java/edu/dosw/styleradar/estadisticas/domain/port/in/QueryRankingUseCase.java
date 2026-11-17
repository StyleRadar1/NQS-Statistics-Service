package edu.dosw.styleradar.estadisticas.domain.port.in;

import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Ranking;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;

public interface QueryRankingUseCase {
    Ranking query(MetricCode metricCode, Scope scope);
}
