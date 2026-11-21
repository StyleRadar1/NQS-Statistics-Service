package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.exception.UnauthorizedScopeException;
import edu.dosw.styleradar.estadisticas.domain.exception.UnsupportedMetricScopeException;
import edu.dosw.styleradar.estadisticas.domain.model.GoalProgressStatus;
import edu.dosw.styleradar.estadisticas.domain.model.GarmentSavesCount;
import edu.dosw.styleradar.estadisticas.domain.model.DistributionCount;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Period;
import edu.dosw.styleradar.estadisticas.domain.model.Point;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.port.out.MetricRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryGoalProgressUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-17T12:00:00Z"), ZoneOffset.UTC);

    private final RecordingMetricRepository repository = new RecordingMetricRepository();
    private QueryGoalProgressUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        repository.points = List.of();
        repository.goalTarget = Optional.empty();
        repository.dailyCalls = 0;
        repository.goalCalls = 0;
        useCase = new QueryGoalProgressUseCaseImpl(repository, CLOCK);
    }

    @Test
    void calculatesProgressForCurrentWeekWhenGoalIsDefined() {
        Scope scope = new Scope("store-1", ScopeType.STORE);
        repository.points = List.of(
                new Point(LocalDate.parse("2026-11-16"), BigDecimal.valueOf(100)),
                new Point(LocalDate.parse("2026-11-17"), BigDecimal.valueOf(50)));
        repository.goalTarget = Optional.of(BigDecimal.valueOf(200));

        var result = useCase.query(MetricCode.STORE_PAGE_VISITS, scope);

        assertEquals(new BigDecimal("150"), result.currentValue());
        assertEquals(new BigDecimal("200"), result.targetValue());
        assertEquals(new BigDecimal("75.0"), result.progressPercentage());
        assertEquals(GoalProgressStatus.GOAL_DEFINED, result.status());
        assertEquals(new Period(LocalDate.parse("2026-11-16"), LocalDate.parse("2026-11-17")),
                repository.lastQueriedPeriod);
        assertEquals(LocalDate.parse("2026-11-16"), repository.lastGoalPeriodStartDate);
        assertEquals(1, repository.dailyCalls);
        assertEquals(1, repository.goalCalls);
    }

    @Test
    void returnsCurrentValueWithoutPercentageWhenGoalIsNotDefined() {
        Scope scope = new Scope("foundation-1", ScopeType.FOUNDATION);
        repository.points = List.of(new Point(LocalDate.parse("2026-11-03"), BigDecimal.valueOf(25)));

        var result = useCase.query(MetricCode.FOUNDATION_GARMENTS_RECEIVED, scope);

        assertEquals(new BigDecimal("25"), result.currentValue());
        assertNull(result.targetValue());
        assertNull(result.progressPercentage());
        assertEquals(GoalProgressStatus.GOAL_NOT_DEFINED, result.status());
        assertEquals("Tu fundación aún no tiene una meta mensual de prendas", result.message());
        assertEquals(new Period(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-17")),
                repository.lastQueriedPeriod);
    }

    @Test
    void definesGoalAndReturnsUpdatedProgress() {
        Scope scope = new Scope("store-1", ScopeType.STORE);
        repository.points = List.of(new Point(LocalDate.parse("2026-11-16"), BigDecimal.valueOf(80)));

        var result = useCase.defineGoal(MetricCode.STORE_PAGE_VISITS, scope, BigDecimal.valueOf(200));

        assertEquals(GoalProgressStatus.GOAL_DEFINED, result.status());
        assertEquals(new BigDecimal("200"), result.targetValue());
        assertEquals(new BigDecimal("40.0"), result.progressPercentage());
        assertEquals(LocalDate.parse("2026-11-16"), repository.lastUpsertedGoalPeriodStart);

        assertThrows(IllegalArgumentException.class, () -> useCase.defineGoal(
                MetricCode.STORE_PAGE_VISITS, scope, BigDecimal.ZERO));
        assertThrows(UnauthorizedScopeException.class, () -> useCase.defineGoal(
                MetricCode.STORE_PAGE_VISITS, new Scope("platform", ScopeType.PLATFORM), BigDecimal.TEN));
    }

    @Test
    void rejectsScopeThatDoesNotMatchTheMetric() {
        assertThrows(UnauthorizedScopeException.class, () -> useCase.query(
                MetricCode.STORE_PAGE_VISITS,
                new Scope("platform", ScopeType.PLATFORM)));

        assertEquals(0, repository.dailyCalls);
        assertEquals(0, repository.goalCalls);
    }

    @Test
    void rejectsUnsupportedGoalMetricAndNonPositiveTargets() {
        assertThrows(UnsupportedMetricScopeException.class, () -> useCase.query(
                MetricCode.STORE_WISHLIST_SAVES,
                new Scope("store-1", ScopeType.STORE)));

        repository.goalTarget = Optional.of(BigDecimal.ZERO);
        assertThrows(IllegalStateException.class, () -> useCase.query(
                MetricCode.PLATFORM_SUBSCRIPTION_REVENUE,
                new Scope("platform", ScopeType.PLATFORM)));
    }

    private static final class RecordingMetricRepository implements MetricRepositoryPort {
        private List<Point> points = List.of();
        private Optional<BigDecimal> goalTarget = Optional.empty();
        private Period lastQueriedPeriod;
        private LocalDate lastGoalPeriodStartDate;
        private LocalDate lastUpsertedGoalPeriodStart;
        private int dailyCalls;
        private int goalCalls;

        @Override
        public List<Point> findDailyPoints(MetricCode metricCode, Scope scope, Period period) {
            dailyCalls++;
            lastQueriedPeriod = period;
            return points;
        }

        @Override
        public Optional<BigDecimal> findGoalTarget(
                MetricCode metricCode,
                Scope scope,
                LocalDate periodStartDate
        ) {
            goalCalls++;
            lastGoalPeriodStartDate = periodStartDate;
            return goalTarget;
        }

        @Override
        public List<GarmentSavesCount> findItemMetricTotals(MetricCode metricCode, Scope scope) {
            return List.of();
        }

        @Override
        public List<DistributionCount> findDistributionCounts(MetricCode metricCode, Scope scope) {
            return List.of();
        }

        @Override
        public void upsertGoalTarget(
                MetricCode metricCode,
                Scope scope,
                LocalDate periodStartDate,
                BigDecimal targetValue
        ) {
            lastUpsertedGoalPeriodStart = periodStartDate;
            goalTarget = Optional.of(targetValue);
        }
    }
}
