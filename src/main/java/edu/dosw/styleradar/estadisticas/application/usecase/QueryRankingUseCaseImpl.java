package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.exception.UnauthorizedScopeException;
import edu.dosw.styleradar.estadisticas.domain.exception.UnsupportedMetricScopeException;
import edu.dosw.styleradar.estadisticas.domain.model.GarmentSavesCount;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.RankedItem;
import edu.dosw.styleradar.estadisticas.domain.model.Ranking;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.port.in.QueryRankingUseCase;
import edu.dosw.styleradar.estadisticas.domain.port.out.MetricRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Service
public class QueryRankingUseCaseImpl implements QueryRankingUseCase {

    private static final int MAX_ITEMS = 6;
    private static final String EMPTY_RANKING_MESSAGE = "No hay prendas publicadas";

    private final MetricRepositoryPort metricRepository;

    public QueryRankingUseCaseImpl(MetricRepositoryPort metricRepository) {
        this.metricRepository = metricRepository;
    }

    @Override
    public Ranking query(MetricCode metricCode, Scope scope) {
        Objects.requireNonNull(metricCode, "Metric code must not be null");
        Objects.requireNonNull(scope, "Authenticated scope must not be null");
        if (metricCode != MetricCode.STORE_GARMENTS_BY_SAVES) {
            throw new UnsupportedMetricScopeException(
                    "Metric " + metricCode + " does not support garment rankings");
        }
        if (scope.type() != ScopeType.STORE || metricCode.scopeType() != scope.type()) {
            throw new UnauthorizedScopeException("The authenticated scope cannot access this metric");
        }

        List<GarmentSavesCount> topItems = metricRepository.findItemMetricTotals(metricCode, scope).stream()
                .sorted(Comparator.comparingLong(GarmentSavesCount::savesCount)
                        .reversed()
                        .thenComparing(GarmentSavesCount::itemId))
                .limit(MAX_ITEMS)
                .toList();
        List<RankedItem> rankedItems = IntStream.range(0, topItems.size())
                .mapToObj(index -> {
                    GarmentSavesCount item = topItems.get(index);
                    return new RankedItem(item.itemId(), item.name(), index + 1, item.savesCount());
                })
                .toList();

        return new Ranking(
                metricCode,
                scope,
                MAX_ITEMS,
                rankedItems,
                rankedItems.isEmpty() ? EMPTY_RANKING_MESSAGE : null);
    }
}
