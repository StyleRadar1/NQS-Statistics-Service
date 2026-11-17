package edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence;

import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.CategoryCode;
import edu.dosw.styleradar.estadisticas.domain.model.DistributionCount;
import edu.dosw.styleradar.estadisticas.domain.model.GarmentSavesCount;
import edu.dosw.styleradar.estadisticas.domain.model.Period;
import edu.dosw.styleradar.estadisticas.domain.model.Point;
import edu.dosw.styleradar.estadisticas.domain.model.PendingModerationPublisherCount;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.port.out.MetricRepositoryPort;
import edu.dosw.styleradar.estadisticas.domain.port.out.PendingModerationRepositoryPort;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository.MetricGoalJpaRepository;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository.MetricItemDailyJpaRepository;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository.MetricDailyJpaRepository;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository.MetricDistributionJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

@Component
public class MetricPersistenceAdapter implements MetricRepositoryPort, PendingModerationRepositoryPort {

    private final MetricDailyJpaRepository metricDailyJpaRepository;
    private final MetricGoalJpaRepository metricGoalJpaRepository;
    private final MetricItemDailyJpaRepository metricItemDailyJpaRepository;
    private final MetricDistributionJpaRepository metricDistributionJpaRepository;

    public MetricPersistenceAdapter(
            MetricDailyJpaRepository metricDailyJpaRepository,
            MetricGoalJpaRepository metricGoalJpaRepository,
            MetricItemDailyJpaRepository metricItemDailyJpaRepository,
            MetricDistributionJpaRepository metricDistributionJpaRepository
    ) {
        this.metricDailyJpaRepository = metricDailyJpaRepository;
        this.metricGoalJpaRepository = metricGoalJpaRepository;
        this.metricItemDailyJpaRepository = metricItemDailyJpaRepository;
        this.metricDistributionJpaRepository = metricDistributionJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Point> findDailyPoints(MetricCode metricCode, Scope scope, Period period) {
        return metricDailyJpaRepository
                .findByScopeTypeAndScopeIdAndMetricCodeAndMetricDateBetweenOrderByMetricDateAsc(
                        scope.type().name(),
                        scope.id(),
                        metricCode.name(),
                        period.startDate(),
                        period.endDate())
                .stream()
                .map(entity -> new Point(entity.getMetricDate(), entity.getValue()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> findGoalTarget(
            MetricCode metricCode,
            Scope scope,
            LocalDate periodStartDate
    ) {
        return metricGoalJpaRepository.findByScopeTypeAndScopeIdAndMetricCodeAndPeriodStartDate(
                        scope.type().name(),
                        scope.id(),
                        metricCode.name(),
                        periodStartDate)
                .map(entity -> entity.getTargetValue());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GarmentSavesCount> findItemMetricTotals(MetricCode metricCode, Scope scope) {
        return metricItemDailyJpaRepository.findItemMetricTotals(
                scope.type().name(),
                scope.id(),
                metricCode.name());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DistributionCount> findDistributionCounts(MetricCode metricCode, Scope scope) {
        return metricDistributionJpaRepository.findByScopeTypeAndScopeIdAndMetricCode(
                        scope.type().name(),
                        scope.id(),
                        metricCode.name())
                .stream()
                .map(entity -> new DistributionCount(
                        new CategoryCode(entity.getCategoryCode()),
                        entity.getValue()))
                .toList();
    }

    @Override
    @Transactional
    public void upsertDailyIncrement(
            MetricCode metricCode,
            Scope scope,
            LocalDate metricDate,
            BigDecimal increment
    ) {
        metricDailyJpaRepository.upsertDailyIncrement(
                UUID.randomUUID(),
                scope.type().name(),
                scope.id(),
                metricCode.name(),
                metricDate,
                increment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingModerationPublisherCount> findPendingModerationPublishers(
            MetricCode metricCode,
            Scope scope,
            List<CategoryCode> publisherTypes
    ) {
        return metricItemDailyJpaRepository.findPendingModerationPublishers(
                        scope.type().name(),
                        scope.id(),
                        metricCode.name(),
                        publisherTypes.stream().map(CategoryCode::value).toList())
                .stream()
                .map(publisher -> new PendingModerationPublisherCount(
                        new CategoryCode(publisher.getPublisherType()),
                        publisher.getPublisherId(),
                        publisher.getPublisherName(),
                        publisher.getPendingCount()))
                .toList();
    }
}
