package edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository;

import edu.dosw.styleradar.estadisticas.domain.model.GarmentSavesCount;
import edu.dosw.styleradar.estadisticas.domain.model.PendingModerationPublisherCount;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.MetricItemDailyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MetricItemDailyJpaRepository extends JpaRepository<MetricItemDailyEntity, UUID> {

    @Query("""
            SELECT new edu.dosw.styleradar.estadisticas.domain.model.GarmentSavesCount(
                item.itemId, MAX(item.itemName), SUM(item.value))
            FROM MetricItemDailyEntity item
            WHERE item.scopeType = :scopeType
              AND item.scopeId = :scopeId
              AND item.metricCode = :metricCode
            GROUP BY item.itemId
            ORDER BY SUM(item.value) DESC, item.itemId ASC
            """)
    List<GarmentSavesCount> findItemMetricTotals(
            @Param("scopeType") String scopeType,
            @Param("scopeId") String scopeId,
            @Param("metricCode") String metricCode);

    @Query("""
            SELECT item.categoryCode AS publisherType,
                   item.itemId AS publisherId,
                   MAX(item.itemName) AS publisherName,
                   SUM(item.value) AS pendingCount
            FROM MetricItemDailyEntity item
            WHERE item.scopeType = :scopeType
              AND item.scopeId = :scopeId
              AND item.metricCode = :metricCode
              AND item.categoryCode IN :publisherTypes
            GROUP BY item.categoryCode, item.itemId
            HAVING SUM(item.value) > 0
            ORDER BY item.categoryCode ASC, MAX(item.itemName) ASC, item.itemId ASC
            """)
    List<PendingModerationPublisherProjection> findPendingModerationPublishers(
            @Param("scopeType") String scopeType,
            @Param("scopeId") String scopeId,
            @Param("metricCode") String metricCode,
            @Param("publisherTypes") List<String> publisherTypes);

    List<MetricItemDailyEntity> findByScopeTypeAndScopeIdAndMetricCodeAndCategoryCodeAndItemIdAndValueGreaterThan(
            String scopeType,
            String scopeId,
            String metricCode,
            String categoryCode,
            String itemId,
            long minValue);

    Optional<MetricItemDailyEntity> findByScopeTypeAndScopeIdAndMetricCodeAndCategoryCodeAndItemIdAndDay(
            String scopeType,
            String scopeId,
            String metricCode,
            String categoryCode,
            String itemId,
            LocalDate day);

    interface PendingModerationPublisherProjection {
        String getPublisherType();

        String getPublisherId();

        String getPublisherName();

        long getPendingCount();
    }
}
