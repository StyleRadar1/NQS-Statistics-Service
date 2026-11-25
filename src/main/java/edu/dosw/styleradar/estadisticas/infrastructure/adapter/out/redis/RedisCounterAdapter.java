package edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.redis;

import edu.dosw.styleradar.estadisticas.domain.model.DailyMetricIncrement;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.port.out.RealTimeCounterPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class RedisCounterAdapter implements RealTimeCounterPort {

    private static final String DAILY_COUNTER_KEY_PREFIX = "statistics:daily";
    private static final String DAILY_COUNTER_KEYS_KEY = "statistics:daily:pending";
    private static final String PROCESSED_EVENT_KEY_PREFIX = "statistics:processed:event";
    private static final DefaultRedisScript<Long> INCREMENT_DAILY_SCRIPT = new DefaultRedisScript<>(
            "redis.call('INCRBYFLOAT', KEYS[1], ARGV[1]); "
                    + "redis.call('SADD', KEYS[2], KEYS[1]); "
                    + "return 1",
            Long.class);
    private static final DefaultRedisScript<List> DRAIN_DAILY_SCRIPT = new DefaultRedisScript<>(
            "local keys = redis.call('SMEMBERS', KEYS[1]); "
                    + "local result = {}; "
                    + "for _, key in ipairs(keys) do "
                    + "local value = redis.call('GET', key); "
                    + "if value then table.insert(result, key); table.insert(result, value); end; "
                    + "redis.call('DEL', key); redis.call('SREM', KEYS[1], key); "
                    + "end; "
                    + "return result",
            List.class);

    private final StringRedisTemplate redisTemplate;

    public RedisCounterAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean markEventProcessed(String eventId, Duration ttl) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Event id must not be blank");
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Event idempotency TTL must be positive");
        }
        Boolean marked = redisTemplate.opsForValue().setIfAbsent(
                PROCESSED_EVENT_KEY_PREFIX + ":" + encode(eventId),
                "1",
                ttl);
        if (marked == null) {
            throw new IllegalStateException("Redis did not confirm event idempotency result");
        }
        return marked;
    }

    @Override
    public void incrementDaily(
            MetricCode metricCode,
            Scope scope,
            LocalDate metricDate,
            BigDecimal amount
    ) {
        if (metricCode.scopeType() != scope.type()) {
            throw new IllegalArgumentException("Metric code and scope types must match");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Counter increment must be positive");
        }
        String key = dailyCounterKey(metricCode, scope, metricDate);
        Long result = redisTemplate.execute(
                INCREMENT_DAILY_SCRIPT,
                List.of(key, DAILY_COUNTER_KEYS_KEY),
                amount.toPlainString());
        if (result == null || result != 1L) {
            throw new IllegalStateException("Redis did not confirm daily counter increment: " + key);
        }
    }

    @Override
    public List<DailyMetricIncrement> drainDailyCounters() {
        List<?> entries = redisTemplate.execute(DRAIN_DAILY_SCRIPT, List.of(DAILY_COUNTER_KEYS_KEY));
        if (entries == null) {
            throw new IllegalStateException("Redis did not return drained daily counters");
        }
        if (entries.size() % 2 != 0) {
            throw new IllegalStateException("Redis returned malformed daily counter data");
        }

        List<DailyMetricIncrement> increments = new ArrayList<>(entries.size() / 2);
        for (int index = 0; index < entries.size(); index += 2) {
            String key = String.valueOf(entries.get(index));
            String value = String.valueOf(entries.get(index + 1));
            increments.add(parseDailyCounter(key, value));
        }
        return List.copyOf(increments);
    }

    private String dailyCounterKey(MetricCode metricCode, Scope scope, LocalDate metricDate) {
        return DAILY_COUNTER_KEY_PREFIX + ":" + metricDate
                + ":" + scope.type().name()
                + ":" + encode(scope.id())
                + ":" + metricCode.name();
    }

    private DailyMetricIncrement parseDailyCounter(String key, String value) {
        String[] parts = key.split(":", -1);
        if (parts.length != 6 || !DAILY_COUNTER_KEY_PREFIX.equals(parts[0] + ":" + parts[1])) {
            throw new IllegalStateException("Redis returned an invalid daily counter key: " + key);
        }
        try {
            LocalDate metricDate = LocalDate.parse(parts[2]);
            ScopeType scopeType = ScopeType.valueOf(parts[3]);
            String scopeId = new String(Base64.getUrlDecoder().decode(parts[4]), StandardCharsets.UTF_8);
            MetricCode metricCode = MetricCode.valueOf(parts[5]);
            return new DailyMetricIncrement(
                    metricCode,
                    new Scope(scopeId, scopeType),
                    metricDate,
                    new BigDecimal(value));
        } catch (IllegalArgumentException | java.time.format.DateTimeParseException exception) {
            throw new IllegalStateException("Redis returned invalid daily counter data: " + key, exception);
        }
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
