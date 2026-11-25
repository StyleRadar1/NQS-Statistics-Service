package edu.dosw.styleradar.estadisticas.domain.port.out;

import edu.dosw.styleradar.estadisticas.domain.model.CategoryCode;
import edu.dosw.styleradar.estadisticas.domain.model.DistributionCount;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Period;
import edu.dosw.styleradar.estadisticas.domain.model.Point;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface MetricRepositoryPort {
    List<Point> findDailyPoints(MetricCode metricCode, Scope scope, Period period);

    List<DistributionCount> findDistributionCounts(MetricCode metricCode, Scope scope);

    default void upsertDailyIncrement(
            MetricCode metricCode,
            Scope scope,
            LocalDate metricDate,
            BigDecimal increment
    ) {
        throw new UnsupportedOperationException("Daily metric increments are not supported");
    }

    default BigDecimal sumAllDailyPoints(MetricCode metricCode, Scope scope) {
        throw new UnsupportedOperationException("Total metric sums are not supported");
    }

    default void adjustDistributionCount(
            MetricCode metricCode,
            Scope scope,
            CategoryCode categoryCode,
            long delta
    ) {
        throw new UnsupportedOperationException("Distribution count updates are not supported");
    }
}
