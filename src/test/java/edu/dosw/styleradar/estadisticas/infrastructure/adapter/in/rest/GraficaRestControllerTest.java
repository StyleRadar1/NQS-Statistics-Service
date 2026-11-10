package edu.dosw.styleradar.estadisticas.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.estadisticas.application.mapper.TableroDtoMapper;
import edu.dosw.styleradar.estadisticas.domain.model.Grafica;
import edu.dosw.styleradar.estadisticas.domain.model.Periodo;
import edu.dosw.styleradar.estadisticas.domain.model.Punto;
import edu.dosw.styleradar.estadisticas.domain.model.Serie;
import edu.dosw.styleradar.estadisticas.domain.model.TipoSerie;
import edu.dosw.styleradar.estadisticas.domain.port.in.ConsultarGraficaUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GraficaRestController.class)
@Import(TableroDtoMapper.class)
class GraficaRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConsultarGraficaUseCase consultarGraficaUseCase;

    @Test
    @DisplayName("API · Swagger: GET /estadisticas/graficas/{id}/series retorna serie diaria de visitas, guardados y mensajes con ceros")
    void testConsultarSeriesActividadDiariaExitoso() throws Exception {
        UUID id = UUID.randomUUID();
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fin = LocalDate.of(2026, 11, 3);
        Periodo periodo = new Periodo(inicio, fin);

        // Serie Visitas: día 1 = 12, día 2 = 0 (sin actividad), día 3 = 5
        Serie visitas = Serie.crearSerieDiariaConCeros(Serie.SERIE_VISITAS, periodo);
        visitas.getPuntos().get(0).setValor(BigDecimal.valueOf(12));
        visitas.getPuntos().get(2).setValor(BigDecimal.valueOf(5));

        // Serie Guardados: todos 0
        Serie guardados = Serie.crearSerieDiariaConCeros(Serie.SERIE_GUARDADOS, periodo);

        // Serie Mensajes: día 2 = 3
        Serie mensajes = Serie.crearSerieDiariaConCeros(Serie.SERIE_MENSAJES, periodo);
        mensajes.getPuntos().get(1).setValor(BigDecimal.valueOf(3));

        List<Serie> seriesMock = List.of(visitas, guardados, mensajes);

        when(consultarGraficaUseCase.consultarSeries(eq(id), eq(TipoSerie.DIARIA), eq(inicio), eq(fin)))
                .thenReturn(seriesMock);

        mockMvc.perform(get("/estadisticas/graficas/{id}/series", id)
                        .param("tipo", "DIARIA")
                        .param("fechaInicio", "2026-11-01")
                        .param("fechaFin", "2026-11-03")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3))

                // Serie Visitas
                .andExpect(jsonPath("$[0].nombre").value("Visitas"))
                .andExpect(jsonPath("$[0].tipo").value("DIARIA"))
                .andExpect(jsonPath("$[0].puntos.length()").value(3))
                .andExpect(jsonPath("$[0].puntos[0].fecha").value("2026-11-01"))
                .andExpect(jsonPath("$[0].puntos[0].valor").value(12))
                .andExpect(jsonPath("$[0].puntos[1].fecha").value("2026-11-02"))
                .andExpect(jsonPath("$[0].puntos[1].valor").value(0)) // Día sin actividad con valor cero
                .andExpect(jsonPath("$[0].puntos[2].fecha").value("2026-11-03"))
                .andExpect(jsonPath("$[0].puntos[2].valor").value(5))

                // Serie Prendas guardadas
                .andExpect(jsonPath("$[1].nombre").value("Prendas guardadas"))
                .andExpect(jsonPath("$[1].puntos.length()").value(3))
                .andExpect(jsonPath("$[1].puntos[0].valor").value(0))
                .andExpect(jsonPath("$[1].puntos[1].valor").value(0))
                .andExpect(jsonPath("$[1].puntos[2].valor").value(0))

                // Serie Mensajes
                .andExpect(jsonPath("$[2].nombre").value("Mensajes"))
                .andExpect(jsonPath("$[2].puntos.length()").value(3))
                .andExpect(jsonPath("$[2].puntos[1].valor").value(3));
    }

    @Test
    @DisplayName("API · Swagger: GET /estadisticas/graficas/{id} retorna gráfica completa con series")
    void testConsultarGraficaCompletaExitoso() throws Exception {
        UUID id = UUID.randomUUID();
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fin = LocalDate.of(2026, 11, 2);
        Periodo periodo = new Periodo(inicio, fin);

        Grafica grafica = Grafica.crearActividadDiariaTiendaConCeros(id, periodo);

        when(consultarGraficaUseCase.consultarGrafica(eq(id), eq(TipoSerie.DIARIA), eq(inicio), eq(fin)))
                .thenReturn(grafica);

        mockMvc.perform(get("/estadisticas/graficas/{id}", id)
                        .param("fechaInicio", "2026-11-01")
                        .param("fechaFin", "2026-11-02")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.titulo").value(Grafica.TITULO_ACTIVIDAD_DIARIA))
                .andExpect(jsonPath("$.series.length()").value(3));
    }

    @Test
    @DisplayName("API · Swagger: GET /estadisticas/graficas/tiendas/{idTienda}/actividad-diaria retorna gráfica especializada de tienda")
    void testConsultarActividadDiariaTiendaExitoso() throws Exception {
        UUID idTienda = UUID.randomUUID();
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fin = LocalDate.of(2026, 11, 2);
        Periodo periodo = new Periodo(inicio, fin);

        Grafica grafica = Grafica.crearActividadDiariaTiendaConCeros(UUID.randomUUID(), periodo);

        when(consultarGraficaUseCase.consultarActividadDiariaTienda(eq(idTienda), eq(inicio), eq(fin)))
                .thenReturn(grafica);

        mockMvc.perform(get("/estadisticas/graficas/tiendas/{idTienda}/actividad-diaria", idTienda)
                        .param("fechaInicio", "2026-11-01")
                        .param("fechaFin", "2026-11-02")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value(Grafica.TITULO_ACTIVIDAD_DIARIA))
                .andExpect(jsonPath("$.series.length()").value(3));
    }

    @Test
    @DisplayName("API · Swagger: GET /estadisticas/graficas/tiendas/{idTienda}/actividad-diaria/series retorna lista de series de actividad")
    void testConsultarSeriesActividadDiariaTiendaExitoso() throws Exception {
        UUID idTienda = UUID.randomUUID();
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fin = LocalDate.of(2026, 11, 2);
        Periodo periodo = new Periodo(inicio, fin);

        Grafica grafica = Grafica.crearActividadDiariaTiendaConCeros(UUID.randomUUID(), periodo);

        when(consultarGraficaUseCase.consultarActividadDiariaTienda(eq(idTienda), eq(inicio), eq(fin)))
                .thenReturn(grafica);

        mockMvc.perform(get("/estadisticas/graficas/tiendas/{idTienda}/actividad-diaria/series", idTienda)
                        .param("fechaInicio", "2026-11-01")
                        .param("fechaFin", "2026-11-02")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }
}
