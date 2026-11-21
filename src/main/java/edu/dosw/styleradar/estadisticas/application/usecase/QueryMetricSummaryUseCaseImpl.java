package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.exception.UnauthorizedScopeException;
import edu.dosw.styleradar.estadisticas.domain.exception.UnsupportedMetricScopeException;
import edu.dosw.styleradar.estadisticas.domain.model.ComparisonWindow;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.MetricSummaryWithVariation;
import edu.dosw.styleradar.estadisticas.domain.model.MetricTotal;
import edu.dosw.styleradar.estadisticas.domain.model.Period;
import edu.dosw.styleradar.estadisticas.domain.model.Point;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.VariationStatus;
import edu.dosw.styleradar.estadisticas.domain.port.in.QueryMetricSummaryUseCase;
import edu.dosw.styleradar.estadisticas.domain.port.out.MetricRepositoryPort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

@Service
public class QueryMetricSummaryUseCaseImpl implements QueryMetricSummaryUseCase {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal NEGATIVE_HUNDRED = new BigDecimal("-100.00");

    private final MetricRepositoryPort metricRepository;
    private final Clock clock;

    public QueryMetricSummaryUseCaseImpl(MetricRepositoryPort metricRepository, Clock clock) {
        this.metricRepository = metricRepository;
        this.clock = clock;
    }

    @Override
    public MetricSummaryWithVariation queryWithVariation(
            MetricCode metricCode,
            Scope scope,
            ComparisonWindow window
    ) {
        Objects.requireNonNull(metricCode, "Metric code must not be null");
        Objects.requireNonNull(scope, "Authenticated scope must not be null");
        Objects.requireNonNull(window, "Comparison window must not be null");

        if (metricCode.scopeType() != scope.type()) {
            throw new UnauthorizedScopeException("The authenticated scope cannot access this metric");
        }
        if (!supportsVariationSummary(metricCode)) {
            throw new UnsupportedMetricScopeException(
                    "Metric " + metricCode + " does not support period comparison summaries");
        }

        LocalDate today = LocalDate.now(clock);
        Period currentPeriod = window.currentPeriod(today);
        Period previousPeriod = window.previousPeriod(today);

        BigDecimal currentTotal = sumPoints(metricCode, scope, currentPeriod);
        BigDecimal previousTotal = sumPoints(metricCode, scope, previousPeriod);

        if (currentTotal.signum() == 0 && previousTotal.signum() == 0) {
            return new MetricSummaryWithVariation(
                    metricCode,
                    scope,
                    window,
                    currentPeriod,
                    previousPeriod,
                    currentTotal,
                    previousTotal,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    VariationStatus.NO_VARIATION_BOTH_ZERO,
                    noVariationMessage(metricCode));
        }

        if (previousTotal.signum() == 0) {
            return new MetricSummaryWithVariation(
                    metricCode,
                    scope,
                    window,
                    currentPeriod,
                    previousPeriod,
                    currentTotal,
                    previousTotal,
                    null,
                    VariationStatus.NOT_CALCULABLE_PREVIOUS_ZERO,
                    notCalculableMessage(metricCode));
        }

        if (currentTotal.signum() == 0) {
            return new MetricSummaryWithVariation(
                    metricCode,
                    scope,
                    window,
                    currentPeriod,
                    previousPeriod,
                    currentTotal,
                    previousTotal,
                    NEGATIVE_HUNDRED,
                    VariationStatus.CALCULATED,
                    null);
        }

        BigDecimal variation = currentTotal
                .subtract(previousTotal)
                .multiply(HUNDRED)
                .divide(previousTotal, 2, RoundingMode.HALF_UP);

        return new MetricSummaryWithVariation(
                metricCode,
                scope,
                window,
                currentPeriod,
                previousPeriod,
                currentTotal,
                previousTotal,
                variation,
                VariationStatus.CALCULATED,
                null);
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
                BigDecimal total = sumPoints(metricCode, scope, monthPeriod);
                yield new MetricTotal(metricCode, scope, monthPeriod, total);
            }
            default -> throw new UnsupportedMetricScopeException(
                    "Metric " + metricCode + " does not support total queries");
        };
    }

    private BigDecimal sumPoints(MetricCode metricCode, Scope scope, Period period) {
        return metricRepository.findDailyPoints(metricCode, scope, period)
                .stream()
                .map(Point::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean supportsVariationSummary(MetricCode metricCode) {
        return metricCode == MetricCode.STORE_PAGE_VISITS
                || metricCode == MetricCode.STORE_MESSAGES_RECEIVED;
    }

    private String noVariationMessage(MetricCode metricCode) {
        return switch (metricCode) {
            case STORE_PAGE_VISITS -> "Sin variación: ambos periodos sin visitas";
            case STORE_MESSAGES_RECEIVED -> "Sin variación: ambos periodos sin mensajes";
            default -> "Sin variación: ambos periodos sin registros";
        };
    }

    private String notCalculableMessage(MetricCode metricCode) {
        return switch (metricCode) {
            case STORE_PAGE_VISITS -> "No calculable: periodo anterior con 0 visitas";
            case STORE_MESSAGES_RECEIVED -> "No calculable: periodo anterior con 0 mensajes";
            default -> "No calculable: periodo anterior con 0 registros";
        };
    }
}
