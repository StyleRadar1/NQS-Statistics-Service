package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.exception.UnauthorizedScopeException;
import edu.dosw.styleradar.estadisticas.domain.exception.UnsupportedMetricScopeException;
import edu.dosw.styleradar.estadisticas.domain.model.CategoryCode;
import edu.dosw.styleradar.estadisticas.domain.model.Distribution;
import edu.dosw.styleradar.estadisticas.domain.model.DistributionCount;
import edu.dosw.styleradar.estadisticas.domain.model.DistributionEntry;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.port.in.QueryDistributionUseCase;
import edu.dosw.styleradar.estadisticas.domain.port.out.MetricRepositoryPort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class QueryDistributionUseCaseImpl implements QueryDistributionUseCase {

    private static final List<CategoryCode> BUYER_PLAN_CATEGORIES = List.of(
            CategoryCode.PLAN_RADAR,
            CategoryCode.PLAN_RADAR_PLUS);
    private static final List<CategoryCode> STORE_PLAN_CATEGORIES = List.of(
            CategoryCode.PLAN_STORE_MONTHLY,
            CategoryCode.SUSPENDED);
    private static final BigDecimal PERCENTAGE_MULTIPLIER = BigDecimal.valueOf(100);

    private final MetricRepositoryPort metricRepository;

    public QueryDistributionUseCaseImpl(MetricRepositoryPort metricRepository) {
        this.metricRepository = metricRepository;
    }

    @Override
    public Distribution query(MetricCode metricCode, Scope scope) {
        Objects.requireNonNull(metricCode, "Metric code must not be null");
        Objects.requireNonNull(scope, "Authenticated scope must not be null");
        if (metricCode.scopeType() != scope.type()) {
            throw new UnauthorizedScopeException("The authenticated scope cannot access this metric");
        }

        List<CategoryCode> categories = categoriesFor(metricCode);
        List<DistributionCount> counts = Objects.requireNonNull(
                metricRepository.findDistributionCounts(metricCode, scope),
                "Distribution counts must not be null");
        Map<CategoryCode, Long> countsByCategory = aggregateCounts(counts);
        if (countsByCategory.keySet().stream().anyMatch(category -> !categories.contains(category))) {
            throw new IllegalStateException("Distribution contains an unsupported category for " + metricCode);
        }
        List<DistributionEntry> entries = toEntries(categories, countsByCategory);
        return new Distribution(metricCode, scope, entries);
    }

    private Map<CategoryCode, Long> aggregateCounts(List<DistributionCount> counts) {
        Map<CategoryCode, Long> countsByCategory = new LinkedHashMap<>();
        for (DistributionCount count : counts) {
            Objects.requireNonNull(count, "Distribution count must not be null");
            countsByCategory.merge(count.categoryCode(), count.count(), Math::addExact);
        }
        return countsByCategory;
    }

    private List<CategoryCode> categoriesFor(MetricCode metricCode) {
        return switch (metricCode) {
            case PLATFORM_BUYERS_BY_PLAN -> BUYER_PLAN_CATEGORIES;
            case PLATFORM_STORES_BY_PLAN -> STORE_PLAN_CATEGORIES;
            default -> throw new UnsupportedMetricScopeException(
                    "Metric " + metricCode + " does not support distribution queries");
        };
    }

    private List<DistributionEntry> toEntries(
            List<CategoryCode> categories,
            Map<CategoryCode, Long> countsByCategory
    ) {
        BigDecimal total = categories.stream()
                .map(category -> BigDecimal.valueOf(countsByCategory.getOrDefault(category, 0L)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return categories.stream()
                .map(category -> {
                    long count = countsByCategory.getOrDefault(category, 0L);
                    BigDecimal percentage = total.signum() == 0
                            ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.valueOf(count)
                                    .multiply(PERCENTAGE_MULTIPLIER)
                                    .divide(total, 2, RoundingMode.HALF_UP);
                    return new DistributionEntry(category, count, percentage);
                })
                .toList();
    }
}
