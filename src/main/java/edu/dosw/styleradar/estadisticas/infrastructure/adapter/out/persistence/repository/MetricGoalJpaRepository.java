package edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository;

import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.MetricGoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface MetricGoalJpaRepository extends JpaRepository<MetricGoalEntity, UUID> {

    Optional<MetricGoalEntity> findByScopeTypeAndScopeIdAndMetricCodeAndPeriodStartDate(
            String scopeType,
            String scopeId,
            String metricCode,
            LocalDate periodStartDate);
}
