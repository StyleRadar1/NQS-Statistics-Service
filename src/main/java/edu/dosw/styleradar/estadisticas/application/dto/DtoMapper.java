package edu.dosw.styleradar.estadisticas.application.dto;

import edu.dosw.styleradar.estadisticas.domain.model.Distribution;
import edu.dosw.styleradar.estadisticas.domain.model.DistributionEntry;
import edu.dosw.styleradar.estadisticas.domain.model.MetricTotal;
import edu.dosw.styleradar.estadisticas.domain.model.Point;
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

    public MetricTotalResponseDto toDto(MetricTotal metricTotal) {
        PeriodDto periodDto = metricTotal.period() == null
                ? null
                : new PeriodDto(metricTotal.period().startDate(), metricTotal.period().endDate());
        return new MetricTotalResponseDto(
                metricTotal.metricCode(),
                metricTotal.scope().type(),
                metricTotal.scope().id(),
                periodDto,
                metricTotal.total().longValue());
    }
}
