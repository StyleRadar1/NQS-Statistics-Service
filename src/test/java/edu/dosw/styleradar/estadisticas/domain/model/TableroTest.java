package edu.dosw.styleradar.estadisticas.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TableroTest {

    @Test
    @DisplayName("Debe crear tablero de tienda con ceros y los 3 indicadores clave")
    void testCrearTableroTiendaConCeros() {
        UUID idDueño = UUID.randomUUID();
        Periodo periodo = new Periodo(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        Tablero tablero = Tablero.crearTableroTiendaConCeros(idDueño, periodo);

        assertNotNull(tablero.getId());
        assertEquals(idDueño, tablero.getIdDueño());
        assertEquals(TipoTablero.TIENDA, tablero.getTipo());
        assertNotNull(tablero.getIndicadores());
        assertEquals(3, tablero.getIndicadores().size());

        Indicador visitas = tablero.getIndicadores().stream()
                .filter(i -> i.getNombre().equals("Visitas a la página"))
                .findFirst().orElse(null);
        assertNotNull(visitas);
        assertEquals(BigDecimal.ZERO, visitas.getValor());
        assertEquals(UnidadMedida.VISITAS, visitas.getUnidad());
        assertEquals(BigDecimal.ZERO.setScale(2), visitas.getComparacion().getVariacionPorcentual());

        Indicador mensajes = tablero.getIndicadores().stream()
                .filter(i -> i.getNombre().equals("Mensajes de clientes"))
                .findFirst().orElse(null);
        assertNotNull(mensajes);
        assertEquals(BigDecimal.ZERO, mensajes.getValor());
        assertEquals(UnidadMedida.MENSAJES, mensajes.getUnidad());

        Indicador wishlist = tablero.getIndicadores().stream()
                .filter(i -> i.getNombre().equals("Prendas guardadas en wishlist"))
                .findFirst().orElse(null);
        assertNotNull(wishlist);
        assertEquals(BigDecimal.ZERO, wishlist.getValor());
        assertEquals(UnidadMedida.PRENDAS, wishlist.getUnidad());
    }

    @Test
    @DisplayName("Debe verificar getters, setters y builders de entidades auxiliares")
    void testEntidadesAuxiliares() {
        UUID id = UUID.randomUUID();
        LocalDate hoy = LocalDate.now();
        BigDecimal valor = BigDecimal.valueOf(42);

        Punto punto = Punto.builder().id(id).fecha(hoy).valor(valor).build();
        assertEquals(id, punto.getId());
        assertEquals(hoy, punto.getFecha());
        assertEquals(valor, punto.getValor());

        Serie serie = Serie.builder()
                .id(id)
                .nombre("Serie Diaria")
                .tipo(TipoSerie.DIARIA)
                .puntos(List.of(punto))
                .build();
        assertEquals("Serie Diaria", serie.getNombre());
        assertEquals(TipoSerie.DIARIA, serie.getTipo());
        assertEquals(1, serie.getPuntos().size());

        Grafica grafica = Grafica.builder()
                .id(id)
                .titulo("Evolución de Visitas")
                .series(List.of(serie))
                .build();
        assertEquals("Evolución de Visitas", grafica.getTitulo());
        assertEquals(1, grafica.getSeries().size());

        Indicador indicador = Indicador.builder()
                .id(id)
                .nombre("Test")
                .valor(valor)
                .unidad(UnidadMedida.CANTIDAD)
                .fecha(LocalDateTime.now())
                .build();
        assertEquals("Test", indicador.getNombre());
        assertEquals(UnidadMedida.CANTIDAD, indicador.getUnidad());
    }
}
