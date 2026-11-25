package edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence;

import edu.dosw.styleradar.estadisticas.domain.model.CategoryCode;
import edu.dosw.styleradar.estadisticas.domain.model.DistributionCount;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Period;
import edu.dosw.styleradar.estadisticas.domain.model.Point;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.port.out.MetricRepositoryPort;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.MetricDistributionEntity;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository.MetricDailyJpaRepository;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository.MetricDistributionJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
public class MetricPersistenceAdapter implements MetricRepositoryPort {

    private final MetricDailyJpaRepository metricDailyJpaRepository;
    private final MetricDistributionJpaRepository metricDistributionJpaRepository;

    public MetricPersistenceAdapter(
            MetricDailyJpaRepository metricDailyJpaRepository,
            MetricDistributionJpaRepository metricDistributionJpaRepository
    ) {
        this.metricDailyJpaRepository = metricDailyJpaRepository;
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
    public BigDecimal sumAllDailyPoints(MetricCode metricCode, Scope scope) {
        BigDecimal total = metricDailyJpaRepository.sumAllByScopeAndMetricCode(
                scope.type().name(),
                scope.id(),
                metricCode.name());
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    @Transactional
    public void adjustDistributionCount(
            MetricCode metricCode,
            Scope scope,
            CategoryCode categoryCode,
            long delta
    ) {
        if (delta == 0L) {
            return;
        }
        metricDistributionJpaRepository
                .findByScopeTypeAndScopeIdAndMetricCodeAndCategoryCode(
                        scope.type().name(),
                        scope.id(),
                        metricCode.name(),
                        categoryCode.value())
                .ifPresentOrElse(
                        existing -> {
                            existing.adjustValue(delta);
                            metricDistributionJpaRepository.save(existing);
                        },
                        () -> {
                            if (delta > 0L) {
                                metricDistributionJpaRepository.save(new MetricDistributionEntity(
                                        UUID.randomUUID(),
                                        scope.type().name(),
                                        scope.id(),
                                        metricCode.name(),
                                        categoryCode.value(),
                                        delta));
                            }
                        });
    }
}
