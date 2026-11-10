package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.exception.GraficaNotFoundException;
import edu.dosw.styleradar.estadisticas.domain.exception.InvalidPeriodoException;
import edu.dosw.styleradar.estadisticas.domain.model.TipoSerie;
import edu.dosw.styleradar.estadisticas.domain.model.TipoTablero;
import edu.dosw.styleradar.estadisticas.domain.port.out.ConsultarTemporalidadPort;
import edu.dosw.styleradar.estadisticas.domain.port.out.TableroRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarGraficaNegativeTest {

    @Mock
    private ConsultarTemporalidadPort consultarTemporalidadPort;

    @Mock
    private TableroRepositoryPort tableroRepositoryPort;

    private ConsultarGraficaUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarGraficaUseCaseImpl(consultarTemporalidadPort, tableroRepositoryPort);
    }

    @Test
    @DisplayName("Caso negativo: Id de gráfica nulo lanza IllegalArgumentException")
    void testIdGraficaNulo() {
        LocalDate hoy = LocalDate.now();

        assertThrows(IllegalArgumentException.class,
                () -> useCase.consultarGrafica(null, TipoSerie.DIARIA, hoy, hoy));
    }

    @Test
    @DisplayName("Caso negativo: IdDueño nulo en actividad diaria lanza IllegalArgumentException")
    void testIdDuenoNulo() {
        LocalDate hoy = LocalDate.now();

        assertThrows(IllegalArgumentException.class,
                () -> useCase.consultarActividadDiariaTienda(null, hoy, hoy));
    }

    @Test
    @DisplayName("Caso negativo: Sólo fechaInicio sin fechaFin lanza InvalidPeriodoException")
    void testSoloFechaInicioLanzaExcepcion() {
        UUID id = UUID.randomUUID();
        LocalDate hoy = LocalDate.now();

        assertThrows(InvalidPeriodoException.class,
                () -> useCase.consultarGrafica(id, TipoSerie.DIARIA, hoy, null));
    }

    @Test
    @DisplayName("Caso negativo: Sólo fechaFin sin fechaInicio lanza InvalidPeriodoException")
    void testSoloFechaFinLanzaExcepcion() {
        UUID id = UUID.randomUUID();
        LocalDate hoy = LocalDate.now();

        assertThrows(InvalidPeriodoException.class,
                () -> useCase.consultarGrafica(id, TipoSerie.DIARIA, null, hoy));
    }

    @Test
    @DisplayName("Caso negativo: FechaInicio posterior a FechaFin lanza InvalidPeriodoException")
    void testFechaInicioPosteriorAFechaFin() {
        UUID id = UUID.randomUUID();
        LocalDate inicio = LocalDate.of(2026, 11, 10);
        LocalDate fin = LocalDate.of(2026, 11, 1);

        assertThrows(InvalidPeriodoException.class,
                () -> useCase.consultarGrafica(id, TipoSerie.DIARIA, inicio, fin));
    }

    @Test
    @DisplayName("Caso negativo: Gráfica no encontrada ni por ID ni como tienda lanza GraficaNotFoundException")
    void testGraficaNoEncontrada() {
        UUID idInexistente = UUID.randomUUID();
        LocalDate hoy = LocalDate.now();

        when(consultarTemporalidadPort.findGraficaById(idInexistente))
                .thenReturn(Optional.empty());
        when(consultarTemporalidadPort.findTableroByTipoAndIdDueño(TipoTablero.TIENDA, idInexistente))
                .thenReturn(Optional.empty());

        assertThrows(GraficaNotFoundException.class,
                () -> useCase.consultarGrafica(idInexistente, TipoSerie.DIARIA, hoy, hoy));
    }
}
