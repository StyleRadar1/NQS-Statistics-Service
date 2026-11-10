package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.exception.InvalidPeriodoException;
import edu.dosw.styleradar.estadisticas.domain.model.TipoTablero;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConsultarTableroNegativeTest {

    private ConsultarTableroUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarTableroUseCaseImpl(null);
    }

    @Test
    @DisplayName("Caso negativo: Tipo de tablero nulo lanza IllegalArgumentException")
    void testTipoNulo() {
        UUID id = UUID.randomUUID();
        LocalDate hoy = LocalDate.now();

        assertThrows(IllegalArgumentException.class,
                () -> useCase.consultarTablero(null, id, hoy, hoy));
    }

    @Test
    @DisplayName("Caso negativo: IdDueño nulo lanza IllegalArgumentException")
    void testIdDueñoNulo() {
        LocalDate hoy = LocalDate.now();

        assertThrows(IllegalArgumentException.class,
                () -> useCase.consultarTablero(TipoTablero.TIENDA, null, hoy, hoy));
    }

    @Test
    @DisplayName("Caso negativo: Sólo fechaInicio sin fechaFin lanza InvalidPeriodoException")
    void testSoloFechaInicio() {
        UUID id = UUID.randomUUID();
        LocalDate hoy = LocalDate.now();

        assertThrows(InvalidPeriodoException.class,
                () -> useCase.consultarTablero(TipoTablero.TIENDA, id, hoy, null));
    }

    @Test
    @DisplayName("Caso negativo: Sólo fechaFin sin fechaInicio lanza InvalidPeriodoException")
    void testSoloFechaFin() {
        UUID id = UUID.randomUUID();
        LocalDate hoy = LocalDate.now();

        assertThrows(InvalidPeriodoException.class,
                () -> useCase.consultarTablero(TipoTablero.TIENDA, id, null, hoy));
    }

    @Test
    @DisplayName("Caso negativo: fechaInicio posterior a fechaFin lanza InvalidPeriodoException")
    void testFechaInicioPosteriorAFechaFin() {
        UUID id = UUID.randomUUID();
        LocalDate inicio = LocalDate.of(2026, 10, 31);
        LocalDate fin = LocalDate.of(2026, 10, 1);

        assertThrows(InvalidPeriodoException.class,
                () -> useCase.consultarTablero(TipoTablero.TIENDA, id, inicio, fin));
    }
}
