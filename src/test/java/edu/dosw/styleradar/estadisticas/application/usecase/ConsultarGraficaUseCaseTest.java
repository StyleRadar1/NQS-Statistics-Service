package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.model.Grafica;
import edu.dosw.styleradar.estadisticas.domain.model.Periodo;
import edu.dosw.styleradar.estadisticas.domain.model.Punto;
import edu.dosw.styleradar.estadisticas.domain.model.Serie;
import edu.dosw.styleradar.estadisticas.domain.model.Tablero;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultarGraficaUseCaseTest {

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
    @DisplayName("Unitaria · caso de uso: Tienda sin registros previos genera actividad diaria con ceros para el intervalo")
    void testConsultarActividadDiariaTiendaNueva() {
        UUID idDueño = UUID.randomUUID();
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fin = LocalDate.of(2026, 11, 5);

        when(tableroRepositoryPort.findByTipoAndIdDueño(TipoTablero.TIENDA, idDueño))
                .thenReturn(Optional.empty());
        when(tableroRepositoryPort.save(any(Tablero.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(consultarTemporalidadPort.findGraficaByTableroIdAndTitulo(any(UUID.class), eq(Grafica.TITULO_ACTIVIDAD_DIARIA)))
                .thenReturn(Optional.empty());
        when(consultarTemporalidadPort.saveGrafica(any(Grafica.class), any(UUID.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Grafica resultado = useCase.consultarActividadDiariaTienda(idDueño, inicio, fin);

        assertNotNull(resultado);
        assertEquals(Grafica.TITULO_ACTIVIDAD_DIARIA, resultado.getTitulo());
        assertEquals(3, resultado.getSeries().size());

        List<String> nombres = resultado.getSeries().stream().map(Serie::getNombre).toList();
        assertTrue(nombres.contains(Serie.SERIE_VISITAS));
        assertTrue(nombres.contains(Serie.SERIE_GUARDADOS));
        assertTrue(nombres.contains(Serie.SERIE_MENSAJES));

        // Todos los días deben estar presentes con valor cero
        for (Serie serie : resultado.getSeries()) {
            assertEquals(5, serie.getPuntos().size());
            for (Punto punto : serie.getPuntos()) {
                assertEquals(BigDecimal.ZERO, punto.getValor());
            }
        }

        verify(tableroRepositoryPort, times(1)).save(any(Tablero.class));
        verify(consultarTemporalidadPort, times(1)).saveGrafica(any(Grafica.class), any(UUID.class));
    }

    @Test
    @DisplayName("Unitaria · caso de uso: Día específico sin actividad se muestra con valor cero sin omitirse")
    void testConsultarSeriesDiaSinActividadMuestraCero() {
        UUID idGrafica = UUID.randomUUID();
        UUID idSerieVisitas = UUID.randomUUID();
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fin = LocalDate.of(2026, 11, 3);

        // Día 1 tiene actividad (10 visitas). Día 2 y 3 NO tienen actividad.
        Punto p1 = Punto.builder().id(UUID.randomUUID()).fecha(inicio).valor(BigDecimal.valueOf(10)).build();
        Serie serieVisitas = Serie.builder()
                .id(idSerieVisitas)
                .nombre("Visitas")
                .tipo(TipoSerie.DIARIA)
                .puntos(new ArrayList<>(List.of(p1)))
                .build();

        Grafica graficaExistente = Grafica.builder()
                .id(idGrafica)
                .titulo("Actividad diaria de la tienda")
                .series(new ArrayList<>(List.of(serieVisitas)))
                .build();

        when(consultarTemporalidadPort.findGraficaById(idGrafica))
                .thenReturn(Optional.of(graficaExistente));
        when(consultarTemporalidadPort.findPuntosBySerieIdAndPeriodo(idSerieVisitas, inicio, fin))
                .thenReturn(List.of(p1));

        List<Serie> series = useCase.consultarSeries(idGrafica, TipoSerie.DIARIA, inicio, fin);

        assertNotNull(series);
        assertEquals(3, series.size()); // Visitas, Prendas guardadas y Mensajes asegurados

        Serie visitas = series.stream().filter(s -> s.getNombre().equalsIgnoreCase("Visitas")).findFirst().orElseThrow();
        assertEquals(3, visitas.getPuntos().size());

        // Día 1 con 10
        assertEquals(LocalDate.of(2026, 11, 1), visitas.getPuntos().get(0).getFecha());
        assertEquals(BigDecimal.valueOf(10), visitas.getPuntos().get(0).getValor());

        // Día 2 sin actividad -> valor cero NO omitido
        assertEquals(LocalDate.of(2026, 11, 2), visitas.getPuntos().get(1).getFecha());
        assertEquals(BigDecimal.ZERO, visitas.getPuntos().get(1).getValor());

        // Día 3 sin actividad -> valor cero NO omitido
        assertEquals(LocalDate.of(2026, 11, 3), visitas.getPuntos().get(2).getFecha());
        assertEquals(BigDecimal.ZERO, visitas.getPuntos().get(2).getValor());
    }

    @Test
    @DisplayName("Unitaria · caso de uso: Fechas nulas resuelven periodo por defecto desde inicio de mes a hoy")
    void testFechasNulasResuelvenPeriodoDefecto() {
        UUID idGrafica = UUID.randomUUID();
        Grafica grafica = Grafica.builder()
                .id(idGrafica)
                .titulo("Actividad")
                .series(new ArrayList<>())
                .build();

        when(consultarTemporalidadPort.findGraficaById(idGrafica))
                .thenReturn(Optional.of(grafica));

        Grafica resultado = useCase.consultarGrafica(idGrafica, TipoSerie.DIARIA, null, null);

        assertNotNull(resultado);
        LocalDate hoy = LocalDate.now();
        int diasEsperados = hoy.getDayOfMonth();

        for (Serie serie : resultado.getSeries()) {
            assertEquals(diasEsperados, serie.getPuntos().size());
        }
    }

    @Test
    @DisplayName("Unitaria · caso de uso: Consulta por id de tienda en endpoint de gráfica la resuelve exitosamente")
    void testConsultarGraficaPorIdTienda() {
        UUID idTienda = UUID.randomUUID();
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fin = LocalDate.of(2026, 11, 2);

        Tablero tableroTienda = Tablero.crearTableroTiendaConCeros(idTienda, new Periodo(inicio, fin));

        // No encontrada como id de gráfica directa, pero sí como tienda
        when(consultarTemporalidadPort.findGraficaById(idTienda))
                .thenReturn(Optional.empty());
        when(consultarTemporalidadPort.findTableroByTipoAndIdDueño(TipoTablero.TIENDA, idTienda))
                .thenReturn(Optional.of(tableroTienda));
        when(tableroRepositoryPort.findByTipoAndIdDueño(TipoTablero.TIENDA, idTienda))
                .thenReturn(Optional.of(tableroTienda));

        when(consultarTemporalidadPort.findGraficaByTableroIdAndTitulo(tableroTienda.getId(), Grafica.TITULO_ACTIVIDAD_DIARIA))
                .thenReturn(Optional.empty());
        when(consultarTemporalidadPort.saveGrafica(any(Grafica.class), eq(tableroTienda.getId())))
                .thenAnswer(inv -> inv.getArgument(0));

        Grafica resultado = useCase.consultarGrafica(idTienda, TipoSerie.DIARIA, inicio, fin);

        assertNotNull(resultado);
        assertEquals(Grafica.TITULO_ACTIVIDAD_DIARIA, resultado.getTitulo());
        assertEquals(3, resultado.getSeries().size());
    }

    @Test
    @DisplayName("Unitaria · caso de uso: Tienda con gráfica existente recupera y asegura puntos de series en el periodo")
    void testConsultarActividadDiariaTiendaConGraficaExistente() {
        UUID idDueño = UUID.randomUUID();
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fin = LocalDate.of(2026, 11, 2);
        Periodo periodo = new Periodo(inicio, fin);

        Tablero tablero = Tablero.crearTableroTiendaConCeros(idDueño, periodo);
        Grafica graficaExistente = Grafica.crearActividadDiariaTiendaConCeros(UUID.randomUUID(), periodo);

        when(tableroRepositoryPort.findByTipoAndIdDueño(TipoTablero.TIENDA, idDueño))
                .thenReturn(Optional.of(tablero));
        when(consultarTemporalidadPort.findGraficaByTableroIdAndTitulo(tablero.getId(), Grafica.TITULO_ACTIVIDAD_DIARIA))
                .thenReturn(Optional.of(graficaExistente));

        Grafica resultado = useCase.consultarActividadDiariaTienda(idDueño, inicio, fin);

        assertNotNull(resultado);
        assertEquals(Grafica.TITULO_ACTIVIDAD_DIARIA, resultado.getTitulo());
        assertEquals(3, resultado.getSeries().size());
    }

    @Test
    @DisplayName("Unitaria · caso de uso: Gráfica diaria que no es de actividad llena días faltantes para sus series")
    void testConsultarGraficaOtraTematica() {
        UUID idGrafica = UUID.randomUUID();
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fin = LocalDate.of(2026, 11, 2);

        Serie serieGeneral = Serie.builder()
                .id(UUID.randomUUID())
                .nombre("Prendas por categoría")
                .tipo(TipoSerie.DIARIA)
                .puntos(new ArrayList<>())
                .build();

        Grafica grafica = Grafica.builder()
                .id(idGrafica)
                .titulo("Inventario general")
                .series(new ArrayList<>(List.of(serieGeneral)))
                .build();

        when(consultarTemporalidadPort.findGraficaById(idGrafica))
                .thenReturn(Optional.of(grafica));

        Grafica resultado = useCase.consultarGrafica(idGrafica, TipoSerie.DIARIA, inicio, fin);

        assertNotNull(resultado);
        assertEquals(1, resultado.getSeries().size());
        assertEquals(2, resultado.getSeries().get(0).getPuntos().size());
    }
}
