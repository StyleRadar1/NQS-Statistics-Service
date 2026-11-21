package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.exception.UnauthorizedScopeException;
import edu.dosw.styleradar.estadisticas.domain.exception.UnsupportedMetricScopeException;
import edu.dosw.styleradar.estadisticas.domain.model.GoalProgress;
import edu.dosw.styleradar.estadisticas.domain.model.GoalProgressStatus;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Period;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.port.in.QueryGoalProgressUseCase;
import edu.dosw.styleradar.estadisticas.domain.port.out.MetricRepositoryPort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;

@Service
public class QueryGoalProgressUseCaseImpl implements QueryGoalProgressUseCase {

    private final MetricRepositoryPort metricRepository;
    private final Clock clock;

    public QueryGoalProgressUseCaseImpl(MetricRepositoryPort metricRepository, Clock clock) {
        this.metricRepository = metricRepository;
        this.clock = clock;
    }

    @Override
    public GoalProgress query(MetricCode metricCode, Scope scope) {
        Objects.requireNonNull(metricCode, "Metric code must not be null");
        Objects.requireNonNull(scope, "Authenticated scope must not be null");
        if (metricCode.scopeType() != scope.type()) {
            throw new UnauthorizedScopeException("The authenticated scope cannot access this metric");
        }

        LocalDate today = LocalDate.now(clock);
        Period period = resolvePeriod(metricCode, today);
        BigDecimal currentValue = metricRepository.findDailyPoints(metricCode, scope, period)
                .stream()
                .map(point -> point.value())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var targetValue = metricRepository.findGoalTarget(metricCode, scope, period.startDate());
        if (targetValue.isEmpty()) {
            return new GoalProgress(
                    metricCode,
                    scope,
                    period,
                    currentValue,
                    null,
                    null,
                    GoalProgressStatus.GOAL_NOT_DEFINED,
                    goalNotDefinedMessage(metricCode));
        }

        BigDecimal target = targetValue.get();
        if (target.signum() <= 0) {
            throw new IllegalStateException("Configured goal target must be positive");
        }

        BigDecimal progressPercentage = currentValue
                .multiply(BigDecimal.valueOf(100))
                .divide(target, 1, RoundingMode.HALF_UP);
        return new GoalProgress(
                metricCode,
                scope,
                period,
                currentValue,
                target,
                progressPercentage,
                GoalProgressStatus.GOAL_DEFINED,
                null);
    }

    @Override
    public GoalProgress defineGoal(MetricCode metricCode, Scope scope, BigDecimal targetValue) {
        Objects.requireNonNull(metricCode, "Metric code must not be null");
        Objects.requireNonNull(scope, "Authenticated scope must not be null");
        Objects.requireNonNull(targetValue, "Target value must not be null");
        if (metricCode.scopeType() != scope.type()) {
            throw new UnauthorizedScopeException("The authenticated scope cannot access this metric");
        }
        if (targetValue.signum() <= 0) {
            throw new IllegalArgumentException("La meta debe ser un valor mayor que cero");
        }

        LocalDate today = LocalDate.now(clock);
        Period period = resolvePeriod(metricCode, today);
        metricRepository.upsertGoalTarget(metricCode, scope, period.startDate(), targetValue);
        return query(metricCode, scope);
    }

    private String goalNotDefinedMessage(MetricCode metricCode) {
        return switch (metricCode) {
            case STORE_PAGE_VISITS -> "Tu tienda aún no tiene una meta semanal de visitas";
            case FOUNDATION_GARMENTS_RECEIVED -> "Tu fundación aún no tiene una meta mensual de prendas";
            case PLATFORM_SUBSCRIPTION_REVENUE -> "No hay una meta mensual de ingresos definida";
            default -> "No existe una meta definida para esta métrica";
        };
    }

    private Period resolvePeriod(MetricCode metricCode, LocalDate today) {
        return switch (metricCode) {
            case STORE_PAGE_VISITS -> new Period(
                    today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    today);
            case FOUNDATION_GARMENTS_RECEIVED, PLATFORM_SUBSCRIPTION_REVENUE -> new Period(
                    YearMonth.from(today).atDay(1),
                    today);
            default -> throw new UnsupportedMetricScopeException(
                    "Metric " + metricCode + " does not support goal progress");
        };
    }
}
