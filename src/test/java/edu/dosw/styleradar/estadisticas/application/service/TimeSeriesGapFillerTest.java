package edu.dosw.styleradar.estadisticas.application.service;

import edu.dosw.styleradar.estadisticas.domain.model.Period;
import edu.dosw.styleradar.estadisticas.domain.model.Point;
import edu.dosw.styleradar.estadisticas.domain.model.TimeGranularity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeSeriesGapFillerTest {

    private final TimeSeriesGapFiller gapFiller = new TimeSeriesGapFiller();

    @Test
    void fillsMissingDaysAndPreservesChronologicalOrder() {
        Period period = new Period(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-07"));
        List<Point> recovered = List.of(
                new Point(LocalDate.parse("2026-11-02"), BigDecimal.valueOf(4)),
                new Point(LocalDate.parse("2026-11-06"), BigDecimal.valueOf(9)));

        List<Point> result = gapFiller.fill(recovered, period, TimeGranularity.DAY);

        assertEquals(7, result.size());
        assertEquals(LocalDate.parse("2026-11-01"), result.getFirst().date());
        assertEquals(LocalDate.parse("2026-11-07"), result.getLast().date());
        assertEquals(5, result.stream().filter(point -> point.value().signum() == 0).count());
        assertEquals(BigDecimal.valueOf(4), result.get(1).value());
        assertEquals(BigDecimal.valueOf(9), result.get(5).value());
    }

    @Test
    void fillsMissingMonthsUsingMonthStartAsPointDate() {
        Period period = new Period(LocalDate.parse("2026-06-01"), LocalDate.parse("2026-11-30"));
        List<Point> recovered = List.of(
                new Point(LocalDate.parse("2026-06-12"), BigDecimal.valueOf(3)),
                new Point(LocalDate.parse("2026-06-15"), BigDecimal.valueOf(2)),
                new Point(LocalDate.parse("2026-10-20"), BigDecimal.valueOf(7)));

        List<Point> result = gapFiller.fill(recovered, period, TimeGranularity.MONTH);

        assertEquals(6, result.size());
        assertEquals(LocalDate.parse("2026-06-01"), result.getFirst().date());
        assertEquals(LocalDate.parse("2026-11-01"), result.getLast().date());
        assertEquals(4, result.stream().filter(point -> point.value().signum() == 0).count());
        assertEquals(BigDecimal.valueOf(5), result.getFirst().value());
        assertEquals(BigDecimal.valueOf(7), result.get(4).value());
    }
}
