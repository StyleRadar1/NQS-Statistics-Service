package edu.dosw.styleradar.estadisticas.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@EqualsAndHashCode
@ToString
public class Comparacion {

    private final BigDecimal valorAnterior;
    private final BigDecimal variacionPorcentual;

    public Comparacion(BigDecimal valorAnterior, BigDecimal variacionPorcentual) {
        this.valorAnterior = valorAnterior != null ? valorAnterior.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.variacionPorcentual = variacionPorcentual != null ? variacionPorcentual.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public static Comparacion calcular(BigDecimal valorActual, BigDecimal valorAnterior) {
        BigDecimal actual = valorActual != null ? valorActual : BigDecimal.ZERO;
        BigDecimal anterior = valorAnterior != null ? valorAnterior : BigDecimal.ZERO;

        if (anterior.compareTo(BigDecimal.ZERO) == 0) {
            if (actual.compareTo(BigDecimal.ZERO) == 0) {
                return new Comparacion(BigDecimal.ZERO, BigDecimal.ZERO);
            }
            return new Comparacion(BigDecimal.ZERO, BigDecimal.valueOf(100.00));
        }

        BigDecimal diferencia = actual.subtract(anterior);
        BigDecimal variacion = diferencia.divide(anterior, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        return new Comparacion(anterior, variacion);
    }
}
