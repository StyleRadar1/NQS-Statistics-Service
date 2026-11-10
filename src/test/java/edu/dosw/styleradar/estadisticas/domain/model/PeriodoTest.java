package edu.dosw.styleradar.estadisticas.domain.model;

import edu.dosw.styleradar.estadisticas.domain.exception.InvalidPeriodoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PeriodoTest {

    @Test
    @DisplayName("Debe crear un periodo válido y calcular días correctamente")
    void testCrearPeriodoValido() {
        LocalDate inicio = LocalDate.of(2026, 10, 1);
        LocalDate fin = LocalDate.of(2026, 10, 31);

        Periodo periodo = new Periodo(inicio, fin);

        assertEquals(inicio, periodo.getInicio());
        assertEquals(fin, periodo.getFin());
        assertEquals(31, periodo.getDias());
    }

    @Test
    @DisplayName("Debe calcular el periodo anterior con la misma duración")
    void testCalcularPeriodoAnterior() {
        LocalDate inicio = LocalDate.of(2026, 10, 1);
        LocalDate fin = LocalDate.of(2026, 10, 10); // 10 días
        Periodo actual = new Periodo(inicio, fin);

        Periodo anterior = actual.calcularPeriodoAnterior();

        assertEquals(LocalDate.of(2026, 9, 30), anterior.getFin());
        assertEquals(LocalDate.of(2026, 9, 21), anterior.getInicio());
        assertEquals(actual.getDias(), anterior.getDias());
    }

    @Test
    @DisplayName("Debe lanzar excepción si fecha inicio es posterior a fecha fin")
    void testFechaInicioPosteriorAFechaFin() {
        LocalDate inicio = LocalDate.of(2026, 10, 31);
        LocalDate fin = LocalDate.of(2026, 10, 1);

        InvalidPeriodoException ex = assertThrows(InvalidPeriodoException.class,
                () -> new Periodo(inicio, fin));
        assertTrue(ex.getMessage().contains("no puede ser posterior"));
    }

    @Test
    @DisplayName("Debe lanzar excepción si alguna fecha es nula")
    void testFechasNulas() {
        LocalDate fecha = LocalDate.of(2026, 10, 1);

        assertThrows(InvalidPeriodoException.class, () -> new Periodo(null, fecha));
        assertThrows(InvalidPeriodoException.class, () -> new Periodo(fecha, null));
    }

    @Test
    @DisplayName("Debe cumplir equals, hashCode y toString")
    void testEqualsHashCodeToString() {
        LocalDate inicio = LocalDate.of(2026, 10, 1);
        LocalDate fin = LocalDate.of(2026, 10, 15);

        Periodo p1 = new Periodo(inicio, fin);
        Periodo p2 = new Periodo(inicio, fin);

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertNotNull(p1.toString());
    }
}
