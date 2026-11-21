package edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.redis;

import edu.dosw.styleradar.estadisticas.domain.model.DailyMetricIncrement;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisCounterAdapterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisCounterAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RedisCounterAdapter(redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void readsCounterUsingMetricAndAuthenticatedScopeKey() {
        when(valueOperations.get(
                "statistics:realtime:active:STORE:store-17:STORE_REALTIME_VISITORS"))
                .thenReturn("9");

        long count = adapter.countActive(
                MetricCode.STORE_REALTIME_VISITORS,
                new Scope("store-17", ScopeType.STORE));

        assertEquals(9, count);
    }

    @Test
    void returnsZeroWhenTheRedisCounterHasExpiredOrIsMissing() {
        when(valueOperations.get(
                "statistics:realtime:active:PLATFORM:platform:PLATFORM_ACTIVE_USERS"))
                .thenReturn(null);

        long count = adapter.countActive(
                MetricCode.PLATFORM_ACTIVE_USERS,
                new Scope("platform", ScopeType.PLATFORM));

        assertEquals(0, count);
    }

    @Test
    void rejectsMalformedRedisCounterValues() {
        when(valueOperations.get(
                "statistics:realtime:active:STORE:store-17:STORE_REALTIME_VISITORS"))
                .thenReturn("not-a-count");

        assertThrows(IllegalStateException.class, () -> adapter.countActive(
                MetricCode.STORE_REALTIME_VISITORS,
                new Scope("store-17", ScopeType.STORE)));
    }

    @Test
    void rejectsNegativeRedisCounterValues() {
        when(valueOperations.get(
                "statistics:realtime:active:STORE:store-17:STORE_REALTIME_VISITORS"))
                .thenReturn("-1");

        assertThrows(IllegalStateException.class, () -> adapter.countActive(
                MetricCode.STORE_REALTIME_VISITORS,
                new Scope("store-17", ScopeType.STORE)));
    }

    @Test
    void marksEventProcessedAndValidatesArguments() {
        when(valueOperations.setIfAbsent(any(), eq("1"), eq(Duration.ofHours(24))))
                .thenReturn(true, false, null);

        assertTrue(adapter.markEventProcessed("evt-1", Duration.ofHours(24)));
        assertFalse(adapter.markEventProcessed("evt-1", Duration.ofHours(24)));
        assertThrows(IllegalStateException.class, () -> adapter.markEventProcessed("evt-1", Duration.ofHours(24)));

        assertThrows(IllegalArgumentException.class, () -> adapter.markEventProcessed(null, Duration.ofHours(1)));
        assertThrows(IllegalArgumentException.class, () -> adapter.markEventProcessed("  ", Duration.ofHours(1)));
        assertThrows(IllegalArgumentException.class, () -> adapter.markEventProcessed("evt-1", null));
        assertThrows(IllegalArgumentException.class, () -> adapter.markEventProcessed("evt-1", Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> adapter.markEventProcessed("evt-1", Duration.ofSeconds(-1)));
    }

    @Test
    void incrementsActiveCounterAndValidatesMetricAndAmount() {
        Scope storeScope = new Scope("store-17", ScopeType.STORE);
        String key = "statistics:realtime:active:STORE:store-17:STORE_REALTIME_VISITORS";
        when(valueOperations.increment(key, 3L)).thenReturn(3L, (Long) null);

        adapter.incrementActive(MetricCode.STORE_REALTIME_VISITORS, storeScope, 3L);
        verify(valueOperations).increment(key, 3L);

        assertThrows(IllegalStateException.class,
                () -> adapter.incrementActive(MetricCode.STORE_REALTIME_VISITORS, storeScope, 3L));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.incrementActive(MetricCode.STORE_PAGE_VISITS, storeScope, 1L));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.incrementActive(MetricCode.STORE_REALTIME_VISITORS, storeScope, 0L));
    }

    @Test
    void adjustsActiveCounterWithPositiveNegativeZeroAndValidatesMetric() {
        Scope platformScope = new Scope("platform", ScopeType.PLATFORM);
        when(redisTemplate.execute(any(RedisScript.class),
                eq(List.of("statistics:realtime:active:PLATFORM:platform:PLATFORM_ACTIVE_FOUNDATIONS")),
                eq("1"))).thenReturn(5L);
        when(redisTemplate.execute(any(RedisScript.class),
                eq(List.of("statistics:realtime:active:PLATFORM:platform:PLATFORM_ACTIVE_FOUNDATIONS")),
                eq("-1"))).thenReturn(4L, (Long) null);

        adapter.adjustActive(MetricCode.PLATFORM_ACTIVE_FOUNDATIONS, platformScope, 1L);
        adapter.adjustActive(MetricCode.PLATFORM_ACTIVE_FOUNDATIONS, platformScope, -1L);
        adapter.adjustActive(MetricCode.PLATFORM_ACTIVE_FOUNDATIONS, platformScope, 0L);

        assertThrows(IllegalStateException.class,
                () -> adapter.adjustActive(MetricCode.PLATFORM_ACTIVE_FOUNDATIONS, platformScope, -1L));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.adjustActive(MetricCode.PLATFORM_SEARCHES, platformScope, 1L));
    }

    @Test
    void incrementsDailyCounterAndValidatesArgumentsAndScriptResult() {
        Scope storeScope = new Scope("store-17", ScopeType.STORE);
        LocalDate date = LocalDate.of(2026, 11, 17);
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), eq("5")))
                .thenReturn(1L, 0L, null);

        adapter.incrementDaily(MetricCode.STORE_PAGE_VISITS, storeScope, date, new BigDecimal("5"));

        assertThrows(IllegalStateException.class,
                () -> adapter.incrementDaily(MetricCode.STORE_PAGE_VISITS, storeScope, date, new BigDecimal("5")));
        assertThrows(IllegalStateException.class,
                () -> adapter.incrementDaily(MetricCode.STORE_PAGE_VISITS, storeScope, date, new BigDecimal("5")));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.incrementDaily(MetricCode.PLATFORM_SEARCHES, storeScope, date, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.incrementDaily(MetricCode.STORE_PAGE_VISITS, storeScope, date, null));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.incrementDaily(MetricCode.STORE_PAGE_VISITS, storeScope, date, BigDecimal.ZERO));
    }

    @Test
    void drainsDailyCountersAndHandlesMalformedData() {
        String encodedStore = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("store-17".getBytes(StandardCharsets.UTF_8));
        String validKey = "statistics:daily:2026-11-17:STORE:" + encodedStore + ":STORE_PAGE_VISITS";

        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("statistics:daily:pending"))))
                .thenReturn(List.of(validKey, "14.5"));

        List<DailyMetricIncrement> drained = adapter.drainDailyCounters();
        assertEquals(1, drained.size());
        assertEquals(MetricCode.STORE_PAGE_VISITS, drained.getFirst().metricCode());
        assertEquals(new Scope("store-17", ScopeType.STORE), drained.getFirst().scope());
        assertEquals(LocalDate.of(2026, 11, 17), drained.getFirst().metricDate());
        assertEquals(new BigDecimal("14.5"), drained.getFirst().value());

        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("statistics:daily:pending"))))
                .thenReturn(null);
        assertThrows(IllegalStateException.class, () -> adapter.drainDailyCounters());

        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("statistics:daily:pending"))))
                .thenReturn(List.of(validKey));
        assertThrows(IllegalStateException.class, () -> adapter.drainDailyCounters());

        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("statistics:daily:pending"))))
                .thenReturn(List.of("invalid:key", "10"));
        assertThrows(IllegalStateException.class, () -> adapter.drainDailyCounters());

        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("statistics:daily:pending"))))
                .thenReturn(List.of("statistics:daily:not-a-date:STORE:" + encodedStore + ":STORE_PAGE_VISITS", "10"));
        assertThrows(IllegalStateException.class, () -> adapter.drainDailyCounters());
    }
}
