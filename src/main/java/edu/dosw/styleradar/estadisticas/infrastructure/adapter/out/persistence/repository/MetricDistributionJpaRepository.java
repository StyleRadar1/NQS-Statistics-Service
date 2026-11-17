package edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository;

import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.MetricDistributionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MetricDistributionJpaRepository extends JpaRepository<MetricDistributionEntity, UUID> {

    List<MetricDistributionEntity> findByScopeTypeAndScopeIdAndMetricCode(
            String scopeType,
            String scopeId,
            String metricCode);
}
