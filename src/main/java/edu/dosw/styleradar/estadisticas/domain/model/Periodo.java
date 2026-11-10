package edu.dosw.styleradar.estadisticas.domain.model;

import edu.dosw.styleradar.estadisticas.domain.exception.InvalidPeriodoException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Getter
@EqualsAndHashCode
@ToString
public class Periodo {

    private final LocalDate inicio;
    private final LocalDate fin;

    public Periodo(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            throw new InvalidPeriodoException("Las fechas de inicio y fin del periodo no pueden ser nulas");
        }
        if (inicio.isAfter(fin)) {
            throw new InvalidPeriodoException("La fecha de inicio (" + inicio + ") no puede ser posterior a la fecha de fin (" + fin + ")");
        }
        this.inicio = inicio;
        this.fin = fin;
    }

    public long getDias() {
        return ChronoUnit.DAYS.between(inicio, fin) + 1;
    }

    public List<LocalDate> generarDias() {
        return inicio.datesUntil(fin.plusDays(1)).toList();
    }

    public Periodo calcularPeriodoAnterior() {
        long dias = getDias();
        LocalDate finAnterior = inicio.minusDays(1);
        LocalDate inicioAnterior = finAnterior.minusDays(dias - 1);
        return new Periodo(inicioAnterior, finAnterior);
    }
}
