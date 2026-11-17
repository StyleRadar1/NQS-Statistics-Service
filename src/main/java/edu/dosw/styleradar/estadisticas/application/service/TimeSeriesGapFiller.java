package edu.dosw.styleradar.estadisticas.application.service;

import edu.dosw.styleradar.estadisticas.domain.model.Period;
import edu.dosw.styleradar.estadisticas.domain.model.Point;
import edu.dosw.styleradar.estadisticas.domain.model.TimeGranularity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@Service
public class TimeSeriesGapFiller {

    public List<Point> fill(List<Point> recoveredPoints, Period period, TimeGranularity granularity) {
        Objects.requireNonNull(recoveredPoints, "Recovered points must not be null");
        Objects.requireNonNull(period, "Period must not be null");
        Objects.requireNonNull(granularity, "Granularity must not be null");

        return switch (granularity) {
            case DAY -> fillDays(recoveredPoints, period);
            case MONTH -> fillMonths(recoveredPoints, period);
        };
    }

    private List<Point> fillDays(List<Point> recoveredPoints, Period period) {
        Map<LocalDate, BigDecimal> valuesByDate = new TreeMap<>();
        for (Point point : recoveredPoints) {
            if (isWithinPeriod(point.date(), period)) {
                valuesByDate.merge(point.date(), point.value(), BigDecimal::add);
            }
        }

        List<Point> points = new ArrayList<>();
        for (LocalDate date = period.startDate(); !date.isAfter(period.endDate()); date = date.plusDays(1)) {
            points.add(new Point(date, valuesByDate.getOrDefault(date, BigDecimal.ZERO)));
        }
        return List.copyOf(points);
    }

    private List<Point> fillMonths(List<Point> recoveredPoints, Period period) {
        Map<YearMonth, BigDecimal> valuesByMonth = new TreeMap<>();
        for (Point point : recoveredPoints) {
            if (isWithinPeriod(point.date(), period)) {
                valuesByMonth.merge(YearMonth.from(point.date()), point.value(), BigDecimal::add);
            }
        }

        List<Point> points = new ArrayList<>();
        YearMonth lastMonth = YearMonth.from(period.endDate());
        for (YearMonth month = YearMonth.from(period.startDate());
             !month.isAfter(lastMonth);
             month = month.plusMonths(1)) {
            points.add(new Point(month.atDay(1), valuesByMonth.getOrDefault(month, BigDecimal.ZERO)));
        }
        return List.copyOf(points);
    }

    private boolean isWithinPeriod(LocalDate date, Period period) {
        return !date.isBefore(period.startDate()) && !date.isAfter(period.endDate());
    }
}
