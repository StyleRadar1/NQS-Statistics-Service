package edu.dosw.styleradar.estadisticas.application.dto;

import edu.dosw.styleradar.estadisticas.application.dto.response.MonthlyCountDto;
import edu.dosw.styleradar.estadisticas.application.dto.response.MonthlyDonationRequestsResponseDto;
import edu.dosw.styleradar.estadisticas.domain.model.GoalProgress;
import edu.dosw.styleradar.estadisticas.domain.model.Distribution;
import edu.dosw.styleradar.estadisticas.domain.model.DistributionEntry;
import edu.dosw.styleradar.estadisticas.domain.model.MetricSummaryWithVariation;
import edu.dosw.styleradar.estadisticas.domain.model.MetricTotal;
import edu.dosw.styleradar.estadisticas.domain.model.Point;
import edu.dosw.styleradar.estadisticas.domain.model.RankedItem;
import edu.dosw.styleradar.estadisticas.domain.model.Ranking;
import edu.dosw.styleradar.estadisticas.domain.model.RealTimeCount;
import edu.dosw.styleradar.estadisticas.domain.model.TimeSeries;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

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
                goalProgress.status(),
                goalProgress.message());
    }

    public MetricSummaryResponseDto toDto(MetricSummaryWithVariation summary) {
        return new MetricSummaryResponseDto(
                summary.metricCode(),
                summary.scope().type(),
                summary.scope().id(),
                summary.window().code(),
                new PeriodDto(summary.currentPeriod().startDate(), summary.currentPeriod().endDate()),
                new PeriodDto(summary.previousPeriod().startDate(), summary.previousPeriod().endDate()),
                summary.currentTotal(),
                summary.previousTotal(),
                summary.variationPercentage(),
                summary.variationStatus(),
                summary.indicatorMessage());
    }

    public MetricTotalResponseDto toDto(MetricTotal metricTotal) {
        PeriodDto periodDto = metricTotal.period() == null
                ? null
                : new PeriodDto(metricTotal.period().startDate(), metricTotal.period().endDate());
        return new MetricTotalResponseDto(
                metricTotal.metricCode(),
                metricTotal.scope().type(),
                metricTotal.scope().id(),
                periodDto,
                metricTotal.total());
    }

    public MonthlyDonationRequestsResponseDto toMonthlyDonationRequestsDto(
            String foundationId,
            TimeSeries fromUsers,
            TimeSeries fromStores
    ) {
        return new MonthlyDonationRequestsResponseDto(
                foundationId,
                fromUsers.points().stream()
                        .map(point -> new MonthlyCountDto(YearMonth.from(point.date()).toString(), point.value()))
                        .toList(),
                fromStores.points().stream()
                        .map(point -> new MonthlyCountDto(YearMonth.from(point.date()).toString(), point.value()))
                        .toList());
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
