package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.exception.UnauthorizedScopeException;
import edu.dosw.styleradar.estadisticas.domain.exception.UnsupportedMetricScopeException;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.MetricTotal;
import edu.dosw.styleradar.estadisticas.domain.model.Period;
import edu.dosw.styleradar.estadisticas.domain.model.Point;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.port.in.QueryMetricSummaryUseCase;
import edu.dosw.styleradar.estadisticas.domain.port.out.MetricRepositoryPort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

@Service
public class QueryMetricSummaryUseCaseImpl implements QueryMetricSummaryUseCase {

    private final MetricRepositoryPort metricRepository;
    private final Clock clock;

    public QueryMetricSummaryUseCaseImpl(MetricRepositoryPort metricRepository, Clock clock) {
        this.metricRepository = metricRepository;
        this.clock = clock;
    }

    @Override
    public MetricTotal queryTotal(MetricCode metricCode, Scope scope) {
        Objects.requireNonNull(metricCode, "Metric code must not be null");
        Objects.requireNonNull(scope, "Authenticated scope must not be null");
        if (metricCode.scopeType() != scope.type()) {
            throw new UnauthorizedScopeException("The authenticated scope cannot access this metric");
        }

        return switch (metricCode) {
            case FOUNDATION_GARMENTS_RECEIVED,
                    PLATFORM_GARMENT_PUBLICATIONS,
                    PLATFORM_PLAYLISTS_CREATED -> {
                BigDecimal total = metricRepository.sumAllDailyPoints(metricCode, scope);
                yield new MetricTotal(metricCode, scope, null, total != null ? total : BigDecimal.ZERO);
            }
            case PLATFORM_MONTHLY_DONATIONS -> {
                YearMonth currentMonth = YearMonth.from(LocalDate.now(clock));
                Period monthPeriod = new Period(currentMonth.atDay(1), currentMonth.atEndOfMonth());
                BigDecimal total = metricRepository.findDailyPoints(metricCode, scope, monthPeriod)
                        .stream()
                        .map(Point::value)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                yield new MetricTotal(metricCode, scope, monthPeriod, total);
            }
            default -> throw new UnsupportedMetricScopeException(
                    "Metric " + metricCode + " does not support total queries");
        };
    }
}
