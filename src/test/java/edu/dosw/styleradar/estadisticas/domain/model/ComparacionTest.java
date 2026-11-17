package edu.dosw.styleradar.estadisticas.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ComparacionTest {

    @Test
    @DisplayName("Debe calcular variación porcentual positiva correctamente")
    void testVariacionPositiva() {
        BigDecimal actual = BigDecimal.valueOf(150);
        BigDecimal anterior = BigDecimal.valueOf(100);

        Comparacion comparacion = Comparacion.calcular(actual, anterior);

        assertEquals(BigDecimal.valueOf(100).setScale(2), comparacion.getValorAnterior());
        assertEquals(BigDecimal.valueOf(50.00).setScale(2), comparacion.getVariacionPorcentual());
    }

    @Test
    @DisplayName("Debe calcular variación porcentual negativa correctamente")
    void testVariacionNegativa() {
        BigDecimal actual = BigDecimal.valueOf(80);
        BigDecimal anterior = BigDecimal.valueOf(100);

        Comparacion comparacion = Comparacion.calcular(actual, anterior);

        assertEquals(BigDecimal.valueOf(-20.00).setScale(2), comparacion.getVariacionPorcentual());
    }

    @Test
    @DisplayName("Debe retornar 100% si el valor anterior era 0 y el actual es mayor a 0")
    void testValorAnteriorCeroYActualMayorACero() {
        BigDecimal actual = BigDecimal.valueOf(50);
        BigDecimal anterior = BigDecimal.ZERO;

        Comparacion comparacion = Comparacion.calcular(actual, anterior);

        assertEquals(BigDecimal.valueOf(100.00).setScale(2), comparacion.getVariacionPorcentual());
    }

    @Test
    @DisplayName("Debe retornar 0% si ambos valores son cero")
    void testAmbosValoresCero() {
        Comparacion comparacion = Comparacion.calcular(BigDecimal.ZERO, BigDecimal.ZERO);

        assertEquals(BigDecimal.ZERO.setScale(2), comparacion.getValorAnterior());
        assertEquals(BigDecimal.ZERO.setScale(2), comparacion.getVariacionPorcentual());
    }

    @Test
    @DisplayName("Debe manejar valores nulos como cero sin fallar")
    void testValoresNulos() {
        Comparacion comparacion = Comparacion.calcular(null, null);

        assertEquals(BigDecimal.ZERO.setScale(2), comparacion.getValorAnterior());
        assertEquals(BigDecimal.ZERO.setScale(2), comparacion.getVariacionPorcentual());
    }

    @Test
    @DisplayName("Debe cumplir equals, hashCode y toString")
    void testEqualsHashCodeToString() {
        Comparacion c1 = new Comparacion(BigDecimal.valueOf(10), BigDecimal.valueOf(5));
        Comparacion c2 = new Comparacion(BigDecimal.valueOf(10), BigDecimal.valueOf(5));

        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
        assertNotNull(c1.toString());
    }
}
