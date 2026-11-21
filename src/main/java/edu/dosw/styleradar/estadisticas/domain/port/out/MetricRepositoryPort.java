package edu.dosw.styleradar.estadisticas.domain.port.out;

import edu.dosw.styleradar.estadisticas.domain.model.CategoryCode;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.GarmentSavesCount;
import edu.dosw.styleradar.estadisticas.domain.model.DistributionCount;
import edu.dosw.styleradar.estadisticas.domain.model.Period;
import edu.dosw.styleradar.estadisticas.domain.model.Point;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MetricRepositoryPort {
    List<Point> findDailyPoints(MetricCode metricCode, Scope scope, Period period);

    Optional<BigDecimal> findGoalTarget(
            MetricCode metricCode,
            Scope scope,
            LocalDate periodStartDate);

    List<GarmentSavesCount> findItemMetricTotals(MetricCode metricCode, Scope scope);

    List<DistributionCount> findDistributionCounts(MetricCode metricCode, Scope scope);

    default void upsertDailyIncrement(
            MetricCode metricCode,
            Scope scope,
            LocalDate metricDate,
            BigDecimal increment
    ) {
        throw new UnsupportedOperationException("Daily metric increments are not supported");
    }

    default void upsertGoalTarget(
            MetricCode metricCode,
            Scope scope,
            LocalDate periodStartDate,
            BigDecimal targetValue
    ) {
        throw new UnsupportedOperationException("Goal upserts are not supported");
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

    default void upsertItemDailyIncrement(
            MetricCode metricCode,
            Scope scope,
            CategoryCode categoryCode,
            String itemId,
            String itemName,
            LocalDate day,
            long delta
    ) {
        throw new UnsupportedOperationException("Item daily metric updates are not supported");
    }
}
