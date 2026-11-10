package edu.dosw.styleradar.estadisticas.application.mapper;

import edu.dosw.styleradar.estadisticas.application.dto.response.TableroResponseDto;
import edu.dosw.styleradar.estadisticas.domain.model.Comparacion;
import edu.dosw.styleradar.estadisticas.domain.model.Grafica;
import edu.dosw.styleradar.estadisticas.domain.model.Indicador;
import edu.dosw.styleradar.estadisticas.domain.model.Periodo;
import edu.dosw.styleradar.estadisticas.domain.model.Punto;
import edu.dosw.styleradar.estadisticas.domain.model.Serie;
import edu.dosw.styleradar.estadisticas.domain.model.Tablero;
import edu.dosw.styleradar.estadisticas.domain.model.TipoSerie;
import edu.dosw.styleradar.estadisticas.domain.model.TipoTablero;
import edu.dosw.styleradar.estadisticas.domain.model.UnidadMedida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TableroDtoMapperTest {

    private TableroDtoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TableroDtoMapper();
    }

    @Test
    @DisplayName("Debe mapear Tablero dominio completo a TableroResponseDto")
    void testToDtoCompleto() {
        UUID id = UUID.randomUUID();
        UUID idDueño = UUID.randomUUID();
        LocalDate hoy = LocalDate.now();

        Punto punto = Punto.builder().id(UUID.randomUUID()).fecha(hoy).valor(BigDecimal.TEN).build();
        Serie serie = Serie.builder().id(UUID.randomUUID()).nombre("Serie 1").tipo(TipoSerie.DIARIA).puntos(List.of(punto)).build();
        Grafica grafica = Grafica.builder().id(UUID.randomUUID()).titulo("Grafica 1").series(List.of(serie)).build();

        Periodo periodo = new Periodo(hoy.minusDays(7), hoy);
        Comparacion comparacion = new Comparacion(BigDecimal.valueOf(5), BigDecimal.valueOf(100));
        Indicador indicador = Indicador.builder()
                .id(UUID.randomUUID())
                .nombre("Visitas a la página")
                .valor(BigDecimal.TEN)
                .unidad(UnidadMedida.VISITAS)
                .fecha(LocalDateTime.now())
                .periodo(periodo)
                .comparacion(comparacion)
                .build();

        Tablero tablero = Tablero.builder()
                .id(id)
                .idDueño(idDueño)
                .tipo(TipoTablero.TIENDA)
                .indicadores(List.of(indicador))
                .graficas(List.of(grafica))
                .build();

        TableroResponseDto dto = mapper.toDto(tablero);

        assertNotNull(dto);
        assertEquals(id, dto.getId());
        assertEquals(idDueño, dto.getIdDueño());
        assertEquals("TIENDA", dto.getTipo());
        assertEquals(1, dto.getIndicadores().size());
        assertEquals("Visitas a la página", dto.getIndicadores().get(0).getNombre());
        assertEquals("VISITAS", dto.getIndicadores().get(0).getUnidad());
        assertEquals(1, dto.getGraficas().size());
        assertEquals("Grafica 1", dto.getGraficas().get(0).getTitulo());
        assertEquals(1, dto.getGraficas().get(0).getSeries().size());
        assertEquals("Serie 1", dto.getGraficas().get(0).getSeries().get(0).getNombre());

        assertEquals(1, mapper.toIndicadorDtoList(List.of(indicador)).size());
        assertEquals(1, mapper.toGraficaDtoList(List.of(grafica)).size());
    }

    @Test
    @DisplayName("Debe manejar valores nulos de forma segura")
    void testNulls() {
        assertNull(mapper.toDto((Tablero) null));
        assertNull(mapper.toDto((Indicador) null));
        assertNull(mapper.toDto((Periodo) null));
        assertNull(mapper.toDto((Comparacion) null));
        assertNull(mapper.toDto((Grafica) null));
        assertNull(mapper.toDto((Serie) null));
        assertNull(mapper.toDto((Punto) null));
        assertTrue(mapper.toIndicadorDtoList(null).isEmpty());
        assertTrue(mapper.toGraficaDtoList(null).isEmpty());
    }
}
