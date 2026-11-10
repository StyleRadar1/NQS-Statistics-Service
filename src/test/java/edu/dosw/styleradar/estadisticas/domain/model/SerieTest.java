package edu.dosw.styleradar.estadisticas.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SerieTest {

    @Test
    @DisplayName("Unitaria · dominio: Serie vacía genera todos los días del periodo con valor cero sin omitir ninguno")
    void testRellenarDiasFaltantesSerieVacia() {
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fin = LocalDate.of(2026, 11, 4);
        Periodo periodo = new Periodo(inicio, fin);

        Serie serie = Serie.builder()
                .id(UUID.randomUUID())
                .nombre(Serie.SERIE_VISITAS)
                .tipo(TipoSerie.DIARIA)
                .puntos(new ArrayList<>())
                .build();

        serie.rellenarDiasFaltantes(periodo);

        assertEquals(4, serie.getPuntos().size());
        for (int i = 0; i < 4; i++) {
            LocalDate fechaEsperada = inicio.plusDays(i);
            Punto punto = serie.getPuntos().get(i);
            assertEquals(fechaEsperada, punto.getFecha());
            assertEquals(BigDecimal.ZERO, punto.getValor());
            assertNotNull(punto.getId());
        }
    }

    @Test
    @DisplayName("Unitaria · dominio: Preserva puntos existentes y rellena con cero los días sin actividad")
    void testRellenarDiasFaltantesConPuntosExistentes() {
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fin = LocalDate.of(2026, 11, 5);
        Periodo periodo = new Periodo(inicio, fin);

        // Día 1 tiene 15 visitas, Día 4 tiene 8 visitas. Días 2, 3 y 5 sin actividad
        Punto p1 = Punto.builder().id(UUID.randomUUID()).fecha(LocalDate.of(2026, 11, 1)).valor(BigDecimal.valueOf(15)).build();
        Punto p4 = Punto.builder().id(UUID.randomUUID()).fecha(LocalDate.of(2026, 11, 4)).valor(BigDecimal.valueOf(8)).build();

        Serie serie = Serie.builder()
                .id(UUID.randomUUID())
                .nombre(Serie.SERIE_VISITAS)
                .tipo(TipoSerie.DIARIA)
                .puntos(new ArrayList<>(List.of(p4, p1))) // Desordenado a propósito
                .build();

        serie.rellenarDiasFaltantes(periodo);

        assertEquals(5, serie.getPuntos().size());

        // Verificación en orden cronológico
        assertEquals(LocalDate.of(2026, 11, 1), serie.getPuntos().get(0).getFecha());
        assertEquals(BigDecimal.valueOf(15), serie.getPuntos().get(0).getValor());

        assertEquals(LocalDate.of(2026, 11, 2), serie.getPuntos().get(1).getFecha());
        assertEquals(BigDecimal.ZERO, serie.getPuntos().get(1).getValor());

        assertEquals(LocalDate.of(2026, 11, 3), serie.getPuntos().get(2).getFecha());
        assertEquals(BigDecimal.ZERO, serie.getPuntos().get(2).getValor());

        assertEquals(LocalDate.of(2026, 11, 4), serie.getPuntos().get(3).getFecha());
        assertEquals(BigDecimal.valueOf(8), serie.getPuntos().get(3).getValor());

        assertEquals(LocalDate.of(2026, 11, 5), serie.getPuntos().get(4).getFecha());
        assertEquals(BigDecimal.ZERO, serie.getPuntos().get(4).getValor());
    }

    @Test
    @DisplayName("Unitaria · dominio: Si el periodo es nulo, retorna la serie sin modificaciones")
    void testRellenarDiasFaltantesPeriodoNulo() {
        Serie serie = Serie.builder()
                .id(UUID.randomUUID())
                .nombre(Serie.SERIE_MENSAJES)
                .tipo(TipoSerie.DIARIA)
                .puntos(new ArrayList<>())
                .build();

        serie.rellenarDiasFaltantes(null);

        assertTrue(serie.getPuntos().isEmpty());
    }

    @Test
    @DisplayName("Unitaria · dominio: Punto existente con valor nulo se normaliza a cero")
    void testPuntoConValorNuloSeNormalizaACero() {
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fin = LocalDate.of(2026, 11, 1);
        Periodo periodo = new Periodo(inicio, fin);

        Punto puntoNulo = Punto.builder().id(UUID.randomUUID()).fecha(inicio).valor(null).build();
        Serie serie = Serie.builder()
                .id(UUID.randomUUID())
                .nombre(Serie.SERIE_GUARDADOS)
                .tipo(TipoSerie.DIARIA)
                .puntos(new ArrayList<>(List.of(puntoNulo)))
                .build();

        serie.rellenarDiasFaltantes(periodo);

        assertEquals(1, serie.getPuntos().size());
        assertEquals(BigDecimal.ZERO, serie.getPuntos().get(0).getValor());
    }

    @Test
    @DisplayName("Unitaria · dominio: crearSerieDiariaConCeros genera serie con nombre y puntos en cero")
    void testCrearSerieDiariaConCeros() {
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fin = LocalDate.of(2026, 11, 3);
        Periodo periodo = new Periodo(inicio, fin);

        Serie serie = Serie.crearSerieDiariaConCeros(Serie.SERIE_GUARDADOS, periodo);

        assertNotNull(serie.getId());
        assertEquals(Serie.SERIE_GUARDADOS, serie.getNombre());
        assertEquals(TipoSerie.DIARIA, serie.getTipo());
        assertEquals(3, serie.getPuntos().size());
        assertTrue(serie.getPuntos().stream().allMatch(p -> p.getValor().equals(BigDecimal.ZERO)));
    }
}
