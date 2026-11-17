package edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository;

import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.MetricDailyEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MetricDailyJpaRepository extends JpaRepository<MetricDailyEntity, UUID> {

    List<MetricDailyEntity> findByScopeTypeAndScopeIdAndMetricCodeAndMetricDateBetweenOrderByMetricDateAsc(
            String scopeType,
            String scopeId,
            String metricCode,
            LocalDate startDate,
            LocalDate endDate);

    @Modifying
    @Query(value = """
            INSERT INTO metric_daily (id, scope_type, scope_id, metric_code, metric_date, value)
            VALUES (:id, :scopeType, :scopeId, :metricCode, :metricDate, :increment)
            ON CONFLICT (scope_type, scope_id, metric_code, metric_date)
            DO UPDATE SET value = metric_daily.value + EXCLUDED.value
            """, nativeQuery = true)
    int upsertDailyIncrement(
            @Param("id") UUID id,
            @Param("scopeType") String scopeType,
            @Param("scopeId") String scopeId,
            @Param("metricCode") String metricCode,
            @Param("metricDate") LocalDate metricDate,
            @Param("increment") java.math.BigDecimal increment);
}
