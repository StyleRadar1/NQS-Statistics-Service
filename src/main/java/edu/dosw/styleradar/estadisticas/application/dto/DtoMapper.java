package edu.dosw.styleradar.estadisticas.application.dto;

import edu.dosw.styleradar.estadisticas.domain.model.GoalProgress;
import edu.dosw.styleradar.estadisticas.domain.model.Distribution;
import edu.dosw.styleradar.estadisticas.domain.model.DistributionEntry;
import edu.dosw.styleradar.estadisticas.domain.model.Point;
import edu.dosw.styleradar.estadisticas.domain.model.RankedItem;
import edu.dosw.styleradar.estadisticas.domain.model.Ranking;
import edu.dosw.styleradar.estadisticas.domain.model.RealTimeCount;
import edu.dosw.styleradar.estadisticas.domain.model.TimeSeries;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {

    public DistributionResponseDto toDto(Distribution distribution) {
        return new DistributionResponseDto(
                distribution.metricCode(),
                distribution.scope().type(),
                distribution.scope().id(),
                distribution.entries().stream().map(this::toDto).toList());
    }

    public DistributionEntryDto toDto(DistributionEntry entry) {
        return new DistributionEntryDto(
                entry.categoryCode().value(),
                entry.count(),
                entry.percentage());
    }

    public TimeSeriesResponseDto toDto(TimeSeries timeSeries) {
        return new TimeSeriesResponseDto(
                timeSeries.metricCode(),
                timeSeries.scope().type(),
                timeSeries.scope().id(),
                new PeriodDto(timeSeries.period().startDate(), timeSeries.period().endDate()),
                timeSeries.granularity(),
                timeSeries.points().stream().map(this::toDto).toList());
    }

    public PointDto toDto(Point point) {
        return new PointDto(point.date(), point.value());
    }

    public RealTimeCountResponseDto toDto(RealTimeCount realTimeCount) {
        return new RealTimeCountResponseDto(
                realTimeCount.metricCode(),
                realTimeCount.scope().type(),
                realTimeCount.scope().id(),
                realTimeCount.activeCount());
    }

    public GoalProgressResponseDto toDto(GoalProgress goalProgress) {
        return new GoalProgressResponseDto(
                goalProgress.metricCode(),
                goalProgress.scope().type(),
                goalProgress.scope().id(),
                new PeriodDto(goalProgress.period().startDate(), goalProgress.period().endDate()),
                goalProgress.currentValue(),
                goalProgress.targetValue(),
                goalProgress.progressPercentage(),
                goalProgress.status());
    }

    public RankingResponseDto toDto(Ranking ranking) {
        return new RankingResponseDto(
                ranking.metricCode(),
                ranking.scope().type(),
                ranking.scope().id(),
                ranking.limit(),
                ranking.items().stream().map(this::toDto).toList(),
                ranking.message());
    }

    public RankedItemDto toDto(RankedItem rankedItem) {
        return new RankedItemDto(
                rankedItem.itemId(),
                rankedItem.name(),
                rankedItem.position(),
                rankedItem.savesCount());
    }
}
