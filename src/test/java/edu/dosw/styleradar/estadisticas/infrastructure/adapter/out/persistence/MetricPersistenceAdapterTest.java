package edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence;

import edu.dosw.styleradar.estadisticas.domain.model.CategoryCode;
import edu.dosw.styleradar.estadisticas.domain.model.DistributionCount;
import edu.dosw.styleradar.estadisticas.domain.model.GarmentSavesCount;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.PendingModerationPublisherCount;
import edu.dosw.styleradar.estadisticas.domain.model.Period;
import edu.dosw.styleradar.estadisticas.domain.model.Point;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.MetricDailyEntity;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.MetricDistributionEntity;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.MetricGoalEntity;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.MetricItemDailyEntity;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository.MetricDailyJpaRepository;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository.MetricDistributionJpaRepository;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository.MetricGoalJpaRepository;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository.MetricItemDailyJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private MetricGoalJpaRepository metricGoalJpaRepository;

    @Autowired
    private MetricItemDailyJpaRepository metricItemDailyJpaRepository;

    @Autowired
    private MetricDistributionJpaRepository metricDistributionJpaRepository;

    private MetricPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MetricPersistenceAdapter(
                metricDailyJpaRepository,
                metricGoalJpaRepository,
                metricItemDailyJpaRepository,
                metricDistributionJpaRepository);
    }

    @Test
    void findsDailyPointsOrderedByDateWithinPeriod() {
        Scope scope = new Scope("store-17", ScopeType.STORE);
        metricDailyJpaRepository.save(new MetricDailyEntity(
                UUID.randomUUID(),
                "STORE",
                "store-17",
                "STORE_PAGE_VISITS",
                LocalDate.of(2026, 11, 5),
                new BigDecimal("18.00")));
        metricDailyJpaRepository.save(new MetricDailyEntity(
                UUID.randomUUID(),
                "STORE",
                "store-17",
                "STORE_PAGE_VISITS",
                LocalDate.of(2026, 11, 2),
                new BigDecimal("12.00")));

        List<Point> points = adapter.findDailyPoints(
                MetricCode.STORE_PAGE_VISITS,
                scope,
                new Period(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 7)));

        assertEquals(2, points.size());
        assertEquals(LocalDate.of(2026, 11, 2), points.get(0).date());
        assertEquals(0, new BigDecimal("12.00").compareTo(points.get(0).value()));
        assertEquals(LocalDate.of(2026, 11, 5), points.get(1).date());
        assertEquals(0, new BigDecimal("18.00").compareTo(points.get(1).value()));

        assertEquals(0, new BigDecimal("30.00").compareTo(
                adapter.sumAllDailyPoints(MetricCode.STORE_PAGE_VISITS, scope)));
        assertEquals(0, BigDecimal.ZERO.compareTo(
                adapter.sumAllDailyPoints(MetricCode.STORE_MESSAGES_RECEIVED, scope)));
    }

    @Test
    void findsGoalTargetWhenDefinedAndReturnsEmptyWhenMissing() {
        Scope scope = new Scope("store-17", ScopeType.STORE);
        LocalDate start = LocalDate.of(2026, 11, 16);
        metricGoalJpaRepository.save(new MetricGoalEntity(
                UUID.randomUUID(),
                "STORE",
                "store-17",
                "STORE_PAGE_VISITS",
                start,
                new BigDecimal("250.00")));

        Optional<BigDecimal> found = adapter.findGoalTarget(MetricCode.STORE_PAGE_VISITS, scope, start);
        Optional<BigDecimal> missing = adapter.findGoalTarget(MetricCode.STORE_PAGE_VISITS, scope, start.plusWeeks(1));

        assertTrue(found.isPresent());
        assertEquals(0, new BigDecimal("250.00").compareTo(found.get()));
        assertTrue(missing.isEmpty());

        adapter.upsertGoalTarget(MetricCode.STORE_PAGE_VISITS, scope, start, new BigDecimal("300.00"));
        adapter.upsertGoalTarget(MetricCode.STORE_PAGE_VISITS, scope, start.plusWeeks(1), new BigDecimal("400.00"));
        assertEquals(0, new BigDecimal("300.00")
                .compareTo(adapter.findGoalTarget(MetricCode.STORE_PAGE_VISITS, scope, start).orElseThrow()));
        assertEquals(0, new BigDecimal("400.00")
                .compareTo(adapter.findGoalTarget(MetricCode.STORE_PAGE_VISITS, scope, start.plusWeeks(1)).orElseThrow()));
    }

    @Test
    void findsItemMetricTotalsAndPendingModerationPublishers() {
        Scope storeScope = new Scope("store-17", ScopeType.STORE);
        metricItemDailyJpaRepository.save(new MetricItemDailyEntity(
                UUID.randomUUID(),
                "STORE",
                "store-17",
                "STORE_GARMENTS_BY_SAVES",
                "UNSPECIFIED",
                "garment-1",
                "Chaqueta Denim",
                LocalDate.of(2026, 11, 10),
                15L));
        metricItemDailyJpaRepository.save(new MetricItemDailyEntity(
                UUID.randomUUID(),
                "STORE",
                "store-17",
                "STORE_GARMENTS_BY_SAVES",
                "UNSPECIFIED",
                "garment-1",
                "Chaqueta Denim",
                LocalDate.of(2026, 11, 11),
                10L));

        List<GarmentSavesCount> totals = adapter.findItemMetricTotals(MetricCode.STORE_GARMENTS_BY_SAVES, storeScope);
        assertEquals(1, totals.size());
        assertEquals("garment-1", totals.getFirst().itemId());
        assertEquals("Chaqueta Denim", totals.getFirst().name());
        assertEquals(25L, totals.getFirst().savesCount());

        Scope platformScope = new Scope("platform", ScopeType.PLATFORM);
        metricItemDailyJpaRepository.save(new MetricItemDailyEntity(
                UUID.randomUUID(),
                "PLATFORM",
                "platform",
                "MODERATION_PUBLICATIONS",
                "ORIGIN_STORE",
                "store-17",
                "Tienda Aliada",
                LocalDate.of(2026, 11, 17),
                4L));
        metricItemDailyJpaRepository.save(new MetricItemDailyEntity(
                UUID.randomUUID(),
                "PLATFORM",
                "platform",
                "MODERATION_PUBLICATIONS",
                "ORIGIN_USER",
                "user-9",
                "Usuario Reventa",
                LocalDate.of(2026, 11, 17),
                2L));

        List<PendingModerationPublisherCount> publishers = adapter.findPendingModerationPublishers(
                MetricCode.MODERATION_PUBLICATIONS,
                platformScope,
                List.of(CategoryCode.ORIGIN_STORE, CategoryCode.ORIGIN_USER));

        assertEquals(2, publishers.size());
        assertEquals(CategoryCode.ORIGIN_STORE, publishers.get(0).publisherType());
        assertEquals("store-17", publishers.get(0).publisherId());
        assertEquals(4L, publishers.get(0).pendingCount());
        assertEquals(CategoryCode.ORIGIN_USER, publishers.get(1).publisherType());
        assertEquals("user-9", publishers.get(1).publisherId());
        assertEquals(2L, publishers.get(1).pendingCount());

        long resolved = adapter.resolvePendingByPublisher(
                MetricCode.MODERATION_PUBLICATIONS,
                platformScope,
                CategoryCode.ORIGIN_STORE,
                "store-17");
        assertEquals(4L, resolved);

        List<PendingModerationPublisherCount> afterResolve = adapter.findPendingModerationPublishers(
                MetricCode.MODERATION_PUBLICATIONS,
                platformScope,
                List.of(CategoryCode.ORIGIN_STORE, CategoryCode.ORIGIN_USER));
        assertEquals(1, afterResolve.size());
        assertEquals("user-9", afterResolve.getFirst().publisherId());
    }

    @Test
    void findsDistributionCountsAndDelegatesUpsertDailyIncrement() {
        Scope storeScope = new Scope("store-17", ScopeType.STORE);
        metricDistributionJpaRepository.save(new MetricDistributionEntity(
                UUID.randomUUID(),
                "STORE",
                "store-17",
                "STORE_INVENTORY_BY_STATE",
                "AVAILABLE",
                12L));

        List<DistributionCount> counts = adapter.findDistributionCounts(
                MetricCode.STORE_INVENTORY_BY_STATE,
                storeScope);
        assertEquals(1, counts.size());
        assertEquals(CategoryCode.AVAILABLE, counts.getFirst().categoryCode());
        assertEquals(12L, counts.getFirst().count());

        adapter.adjustDistributionCount(MetricCode.STORE_INVENTORY_BY_STATE, storeScope, CategoryCode.AVAILABLE, 3L);
        adapter.adjustDistributionCount(MetricCode.STORE_INVENTORY_BY_STATE, storeScope, CategoryCode.LOW_STOCK, 2L);
        adapter.adjustDistributionCount(MetricCode.STORE_INVENTORY_BY_STATE, storeScope, CategoryCode.LOW_STOCK, -10L);
        adapter.adjustDistributionCount(MetricCode.STORE_INVENTORY_BY_STATE, storeScope, CategoryCode.OUT_OF_STOCK, -1L);
        adapter.adjustDistributionCount(MetricCode.STORE_INVENTORY_BY_STATE, storeScope, CategoryCode.OUT_OF_STOCK, 0L);

        List<DistributionCount> updatedCounts = adapter.findDistributionCounts(
                MetricCode.STORE_INVENTORY_BY_STATE,
                storeScope);
        assertEquals(15L, updatedCounts.stream()
                .filter(c -> c.categoryCode().equals(CategoryCode.AVAILABLE)).findFirst().orElseThrow().count());
        assertEquals(0L, updatedCounts.stream()
                .filter(c -> c.categoryCode().equals(CategoryCode.LOW_STOCK)).findFirst().orElseThrow().count());

        LocalDate itemDay = LocalDate.of(2026, 11, 21);
        adapter.upsertItemDailyIncrement(
                MetricCode.STORE_GARMENTS_BY_SAVES, storeScope, null, "g-new", "Abrigo Lana", itemDay, 4L);
        adapter.upsertItemDailyIncrement(
                MetricCode.STORE_GARMENTS_BY_SAVES, storeScope, null, "g-new", "Abrigo Lana Premium", itemDay, -1L);
        adapter.upsertItemDailyIncrement(
                MetricCode.STORE_GARMENTS_BY_SAVES, storeScope, null, "g-ignored", "Ignorado", itemDay, 0L);
        adapter.upsertItemDailyIncrement(
                MetricCode.STORE_GARMENTS_BY_SAVES, storeScope, null, "g-negative", "Negativo", itemDay, -2L);

        List<GarmentSavesCount> savedItems = adapter.findItemMetricTotals(
                MetricCode.STORE_GARMENTS_BY_SAVES, storeScope);
        assertEquals(1, savedItems.size());
        assertEquals("g-new", savedItems.getFirst().itemId());
        assertEquals("Abrigo Lana Premium", savedItems.getFirst().name());
        assertEquals(3L, savedItems.getFirst().savesCount());

        MetricDailyJpaRepository mockDailyRepo = mock(MetricDailyJpaRepository.class);
        MetricPersistenceAdapter unitAdapter = new MetricPersistenceAdapter(
                mockDailyRepo,
                metricGoalJpaRepository,
                metricItemDailyJpaRepository,
                metricDistributionJpaRepository);

        LocalDate date = LocalDate.of(2026, 11, 17);
        unitAdapter.upsertDailyIncrement(MetricCode.STORE_PAGE_VISITS, storeScope, date, new BigDecimal("5"));
        verify(mockDailyRepo).upsertDailyIncrement(
                any(UUID.class),
                eq("STORE"),
                eq("store-17"),
                eq("STORE_PAGE_VISITS"),
                eq(date),
                eq(new BigDecimal("5")));
    }
}
