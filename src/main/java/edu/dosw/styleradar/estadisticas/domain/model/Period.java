package edu.dosw.styleradar.estadisticas.domain.model;

import java.time.LocalDate;
import java.util.Objects;

public record Period(LocalDate startDate, LocalDate endDate) {

    public Period {
        Objects.requireNonNull(startDate, "Period start date must not be null");
        Objects.requireNonNull(endDate, "Period end date must not be null");
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Period end date must not precede start date");
        }
    }
}
