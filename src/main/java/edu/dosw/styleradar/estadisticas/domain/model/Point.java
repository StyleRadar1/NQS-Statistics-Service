package edu.dosw.styleradar.estadisticas.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record Point(LocalDate date, BigDecimal value) {

    public Point {
        Objects.requireNonNull(date, "Point date must not be null");
        Objects.requireNonNull(value, "Point value must not be null");
    }
}
