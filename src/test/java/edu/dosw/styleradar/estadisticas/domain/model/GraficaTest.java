package edu.dosw.styleradar.estadisticas.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GraficaTest {

    @Test
    @DisplayName("Unitaria · dominio: crearActividadDiariaTiendaConCeros genera las 3 series de actividad con puntos en cero")
    void testCrearActividadDiariaTiendaConCeros() {
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fin = LocalDate.of(2026, 11, 7);
        Periodo periodo = new Periodo(inicio, fin);
        UUID graficaId = UUID.randomUUID();

        Grafica grafica = Grafica.crearActividadDiariaTiendaConCeros(graficaId, periodo);

        assertEquals(graficaId, grafica.getId());
        assertEquals(Grafica.TITULO_ACTIVIDAD_DIARIA, grafica.getTitulo());
        assertEquals(3, grafica.getSeries().size());

        List<String> nombres = grafica.getSeries().stream().map(Serie::getNombre).toList();
        assertTrue(nombres.contains(Serie.SERIE_VISITAS));
        assertTrue(nombres.contains(Serie.SERIE_GUARDADOS));
        assertTrue(nombres.contains(Serie.SERIE_MENSAJES));

        for (Serie serie : grafica.getSeries()) {
            assertEquals(7, serie.getPuntos().size());
            assertEquals(TipoSerie.DIARIA, serie.getTipo());
            for (Punto punto : serie.getPuntos()) {
                assertEquals(BigDecimal.ZERO, punto.getValor());
            }
        }
    }

    @Test
    @DisplayName("Unitaria · dominio: asegurarSeriesActividadDiaria completa series faltantes y preserva existentes")
    void testAsegurarSeriesActividadDiariaParcial() {
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fin = LocalDate.of(2026, 11, 3);
        Periodo periodo = new Periodo(inicio, fin);

        // Serie existente solo con visitas
        Punto p1 = Punto.builder().id(UUID.randomUUID()).fecha(inicio).valor(BigDecimal.valueOf(25)).build();
        Serie serieVisitas = Serie.builder()
                .id(UUID.randomUUID())
                .nombre("Visitas a la página")
                .tipo(TipoSerie.DIARIA)
                .puntos(new ArrayList<>(List.of(p1)))
                .build();

        Grafica grafica = Grafica.builder()
                .id(UUID.randomUUID())
                .titulo("Actividad")
                .series(new ArrayList<>(List.of(serieVisitas)))
                .build();

        grafica.asegurarSeriesActividadDiaria(periodo);

        assertEquals(3, grafica.getSeries().size());

        Serie visitasResult = grafica.getSeries().stream()
                .filter(s -> s.getNombre().toLowerCase().contains("visita"))
                .findFirst().orElseThrow();
        assertEquals(3, visitasResult.getPuntos().size());
        assertEquals(BigDecimal.valueOf(25), visitasResult.getPuntos().get(0).getValor());
        assertEquals(BigDecimal.ZERO, visitasResult.getPuntos().get(1).getValor());
        assertEquals(BigDecimal.ZERO, visitasResult.getPuntos().get(2).getValor());

        Serie guardadosResult = grafica.getSeries().stream()
                .filter(s -> s.getNombre().equals(Serie.SERIE_GUARDADOS))
                .findFirst().orElseThrow();
        assertEquals(3, guardadosResult.getPuntos().size());
        assertTrue(guardadosResult.getPuntos().stream().allMatch(p -> p.getValor().equals(BigDecimal.ZERO)));

        Serie mensajesResult = grafica.getSeries().stream()
                .filter(s -> s.getNombre().equals(Serie.SERIE_MENSAJES))
                .findFirst().orElseThrow();
        assertEquals(3, mensajesResult.getPuntos().size());
        assertTrue(mensajesResult.getPuntos().stream().allMatch(p -> p.getValor().equals(BigDecimal.ZERO)));
    }

    @Test
    @DisplayName("Unitaria · dominio: asegurarSeriesActividadDiaria con lista de series nula inicializa correctamente")
    void testAsegurarSeriesActividadDiariaListaNula() {
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fin = LocalDate.of(2026, 11, 2);
        Periodo periodo = new Periodo(inicio, fin);

        Grafica grafica = Grafica.builder()
                .id(UUID.randomUUID())
                .titulo("Gráfica sin series")
                .series(null)
                .build();

        grafica.asegurarSeriesActividadDiaria(periodo);

        assertNotNull(grafica.getSeries());
        assertEquals(3, grafica.getSeries().size());
    }
}
