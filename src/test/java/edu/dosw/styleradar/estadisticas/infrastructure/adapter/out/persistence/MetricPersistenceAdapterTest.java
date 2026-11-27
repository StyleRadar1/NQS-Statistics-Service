package edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence;

import edu.dosw.styleradar.estadisticas.domain.model.CategoryCode;
import edu.dosw.styleradar.estadisticas.domain.model.DistributionCount;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Period;
import edu.dosw.styleradar.estadisticas.domain.model.Point;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.MetricDailyEntity;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.MetricDistributionEntity;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository.MetricDailyJpaRepository;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository.MetricDistributionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class MetricPersistenceAdapterTest {

    @Autowired
    private MetricDailyJpaRepository metricDailyJpaRepository;

    @Autowired
    private MetricDistributionJpaRepository metricDistributionJpaRepository;

    private MetricPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MetricPersistenceAdapter(metricDailyJpaRepository, metricDistributionJpaRepository);
    }

    @Test
    void findsDailyPointsOrderedByDateWithinPeriodAndSumsAllTime() {
        Scope scope = new Scope("platform", ScopeType.PLATFORM);
        metricDailyJpaRepository.save(new MetricDailyEntity(
                UUID.randomUUID(),
                "PLATFORM",
                "platform",
                "PLATFORM_PLAYLISTS_CREATED",
                LocalDate.of(2026, 11, 5),
                new BigDecimal("18.00")));
        metricDailyJpaRepository.save(new MetricDailyEntity(
                UUID.randomUUID(),
                "PLATFORM",
                "platform",
                "PLATFORM_PLAYLISTS_CREATED",
                LocalDate.of(2026, 11, 2),
                new BigDecimal("12.00")));

        List<Point> points = adapter.findDailyPoints(
                MetricCode.PLATFORM_PLAYLISTS_CREATED,
                scope,
                new Period(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 7)));

        assertEquals(2, points.size());
        assertEquals(LocalDate.of(2026, 11, 2), points.get(0).date());
        assertEquals(0, new BigDecimal("12.00").compareTo(points.get(0).value()));
        assertEquals(LocalDate.of(2026, 11, 5), points.get(1).date());
        assertEquals(0, new BigDecimal("18.00").compareTo(points.get(1).value()));

        assertEquals(0, new BigDecimal("30.00").compareTo(
                adapter.sumAllDailyPoints(MetricCode.PLATFORM_PLAYLISTS_CREATED, scope)));
        assertEquals(0, BigDecimal.ZERO.compareTo(
                adapter.sumAllDailyPoints(MetricCode.PLATFORM_GARMENT_PUBLICATIONS, scope)));
    }

    @Test
    void findsDistributionCountsAndDelegatesUpsertDailyIncrement() {
        Scope platformScope = new Scope("platform", ScopeType.PLATFORM);
        metricDistributionJpaRepository.save(new MetricDistributionEntity(
                UUID.randomUUID(),
                "PLATFORM",
                "platform",
                "PLATFORM_BUYERS_BY_PLAN",
                "PLAN_RADAR",
                12L));

        List<DistributionCount> counts = adapter.findDistributionCounts(
                MetricCode.PLATFORM_BUYERS_BY_PLAN,
                platformScope);
        assertEquals(1, counts.size());
        assertEquals(CategoryCode.PLAN_RADAR, counts.getFirst().categoryCode());
        assertEquals(12L, counts.getFirst().count());

        adapter.adjustDistributionCount(MetricCode.PLATFORM_BUYERS_BY_PLAN, platformScope, CategoryCode.PLAN_RADAR, 3L);
        adapter.adjustDistributionCount(MetricCode.PLATFORM_BUYERS_BY_PLAN, platformScope, CategoryCode.PLAN_RADAR_PLUS, 2L);
        adapter.adjustDistributionCount(MetricCode.PLATFORM_BUYERS_BY_PLAN, platformScope, CategoryCode.PLAN_RADAR_PLUS, -10L);
        adapter.adjustDistributionCount(MetricCode.PLATFORM_STORES_BY_PLAN, platformScope, CategoryCode.SUSPENDED, -1L);
        adapter.adjustDistributionCount(MetricCode.PLATFORM_STORES_BY_PLAN, platformScope, CategoryCode.SUSPENDED, 0L);

        List<DistributionCount> updatedCounts = adapter.findDistributionCounts(
                MetricCode.PLATFORM_BUYERS_BY_PLAN,
                platformScope);
        assertEquals(15L, updatedCounts.stream()
                .filter(c -> c.categoryCode().equals(CategoryCode.PLAN_RADAR)).findFirst().orElseThrow().count());
        assertEquals(0L, updatedCounts.stream()
                .filter(c -> c.categoryCode().equals(CategoryCode.PLAN_RADAR_PLUS)).findFirst().orElseThrow().count());

        MetricDailyJpaRepository mockDailyRepo = mock(MetricDailyJpaRepository.class);
        MetricPersistenceAdapter unitAdapter = new MetricPersistenceAdapter(
                mockDailyRepo,
                metricDistributionJpaRepository);

        LocalDate date = LocalDate.of(2026, 11, 17);
        unitAdapter.upsertDailyIncrement(MetricCode.PLATFORM_USER_REGISTRATIONS, new Scope("platform", ScopeType.PLATFORM), date, new BigDecimal("5"));
        verify(mockDailyRepo).upsertDailyIncrement(
                any(UUID.class),
                eq("PLATFORM"),
                eq("platform"),
                eq("PLATFORM_USER_REGISTRATIONS"),
                eq(date),
                eq(new BigDecimal("5")));
    }
}
