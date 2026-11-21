package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.application.service.TimeSeriesGapFiller;
import edu.dosw.styleradar.estadisticas.domain.exception.InvalidTimeSeriesRequestException;
import edu.dosw.styleradar.estadisticas.domain.exception.UnauthorizedScopeException;
import edu.dosw.styleradar.estadisticas.domain.exception.UnsupportedMetricScopeException;
import edu.dosw.styleradar.estadisticas.domain.model.ComparisonWindow;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Period;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.model.TimeGranularity;
import edu.dosw.styleradar.estadisticas.domain.model.TimeSeries;
import edu.dosw.styleradar.estadisticas.domain.port.in.QueryTimeSeriesUseCase;
import edu.dosw.styleradar.estadisticas.domain.port.out.MetricRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

@Service
public class QueryTimeSeriesUseCaseImpl implements QueryTimeSeriesUseCase {

    private final MetricRepositoryPort metricRepository;
    private final TimeSeriesGapFiller gapFiller;
    private final Clock clock;

    public QueryTimeSeriesUseCaseImpl(
            MetricRepositoryPort metricRepository,
            TimeSeriesGapFiller gapFiller,
            Clock clock
    ) {
        this.metricRepository = metricRepository;
        this.gapFiller = gapFiller;
        this.clock = clock;
    }

    @Override
    public TimeSeries query(
            MetricCode metricCode,
            Scope scope,
            Period requestedPeriod,
            TimeGranularity requestedGranularity
    ) {
        Objects.requireNonNull(metricCode, "Metric code must not be null");
        Objects.requireNonNull(scope, "Authenticated scope must not be null");
        if (metricCode.isRealTime()) {
            throw new UnsupportedMetricScopeException(
                    "Metric " + metricCode + " does not support time-series queries");
        }
        if (metricCode.scopeType() != scope.type()) {
            throw new UnauthorizedScopeException("The authenticated scope cannot access this metric");
        }

        TimeGranularity granularity = requestedGranularity != null
                ? requestedGranularity
                : metricCode.granularity();
        if (granularity != metricCode.granularity()) {
            throw new InvalidTimeSeriesRequestException(
                    "Metric " + metricCode + " requires " + metricCode.granularity() + " granularity");
        }

        Period period = resolvePeriod(metricCode, requestedPeriod);
        if (metricCode.scopeType() == ScopeType.PLATFORM
                && granularity == TimeGranularity.DAY
                && !YearMonth.from(period.startDate()).equals(YearMonth.from(period.endDate()))) {
            throw new InvalidTimeSeriesRequestException("Platform daily metrics must be queried within one month");
        }

        var dailyPoints = metricRepository.findDailyPoints(metricCode, scope, period);
        return new TimeSeries(
                metricCode,
                scope,
                period,
                granularity,
                gapFiller.fill(dailyPoints, period, granularity));
    }

    @Override
    public TimeSeries queryByWindow(MetricCode metricCode, Scope scope, ComparisonWindow window) {
        Objects.requireNonNull(metricCode, "Metric code must not be null");
        Objects.requireNonNull(scope, "Authenticated scope must not be null");
        Objects.requireNonNull(window, "Comparison window must not be null");
        if (metricCode.granularity() != TimeGranularity.DAY || metricCode.scopeType() != ScopeType.STORE) {
            throw new InvalidTimeSeriesRequestException(
                    "El parámetro periodo (7_DIAS, 30_DIAS, 90_DIAS) solo aplica a métricas diarias de tienda");
        }
        Period period = window.currentPeriod(LocalDate.now(clock));
        return query(metricCode, scope, period, TimeGranularity.DAY);
    }

    private Period resolvePeriod(MetricCode metricCode, Period requestedPeriod) {
        LocalDate today = LocalDate.now(clock);
        YearMonth currentMonth = YearMonth.from(today);

        if (metricCode.scopeType() == ScopeType.FOUNDATION) {
            return new Period(
                    currentMonth.minusMonths(5).atDay(1),
                    currentMonth.atEndOfMonth());
        }

        if (requestedPeriod != null) {
            return requestedPeriod;
        }

        if (metricCode == MetricCode.PLATFORM_SUBSCRIPTION_REVENUE) {
            return new Period(
                    currentMonth.minusMonths(5).atDay(1),
                    currentMonth.atEndOfMonth());
        }
        if (metricCode.scopeType() == ScopeType.PLATFORM) {
            return new Period(currentMonth.atDay(1), currentMonth.atEndOfMonth());
        }
        return new Period(currentMonth.atDay(1), today);
    }
}
