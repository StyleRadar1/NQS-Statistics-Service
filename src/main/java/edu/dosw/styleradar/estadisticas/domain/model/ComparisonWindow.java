package edu.dosw.styleradar.estadisticas.domain.model;

import edu.dosw.styleradar.estadisticas.domain.exception.InvalidTimeSeriesRequestException;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

public enum ComparisonWindow {
    DIAS_7("7_DIAS", 7),
    DIAS_30("30_DIAS", 30),
    DIAS_90("90_DIAS", 90);

    private final String code;
    private final int days;

    ComparisonWindow(String code, int days) {
        this.code = code;
        this.days = days;
    }

    public String code() {
        return code;
    }

    public int days() {
        return days;
    }

    public Period currentPeriod(LocalDate today) {
        Objects.requireNonNull(today, "Reference date must not be null");
        return new Period(today.minusDays(days - 1L), today);
    }

    public Period previousPeriod(LocalDate today) {
        Objects.requireNonNull(today, "Reference date must not be null");
        return new Period(today.minusDays((days * 2L) - 1L), today.minusDays(days));
    }

    public static ComparisonWindow fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidTimeSeriesRequestException(
                    "El periodo es obligatorio. Valores permitidos: 7_DIAS, 30_DIAS, 90_DIAS");
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "7_DIAS", "DIAS_7", "7_DAYS", "DAYS_7", "7D" -> DIAS_7;
            case "30_DIAS", "DIAS_30", "30_DAYS", "DAYS_30", "30D" -> DIAS_30;
            case "90_DIAS", "DIAS_90", "90_DAYS", "DAYS_90", "90D" -> DIAS_90;
            default -> throw new InvalidTimeSeriesRequestException(
                    "Periodo inválido '" + raw + "'. Valores permitidos: 7_DIAS, 30_DIAS, 90_DIAS");
        };
    }
}
