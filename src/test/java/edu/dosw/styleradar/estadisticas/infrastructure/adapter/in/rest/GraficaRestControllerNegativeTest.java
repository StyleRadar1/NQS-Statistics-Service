package edu.dosw.styleradar.estadisticas.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.estadisticas.application.mapper.TableroDtoMapper;
import edu.dosw.styleradar.estadisticas.domain.exception.GraficaNotFoundException;
import edu.dosw.styleradar.estadisticas.domain.exception.InvalidPeriodoException;
import edu.dosw.styleradar.estadisticas.domain.model.Grafica;
import edu.dosw.styleradar.estadisticas.domain.model.Serie;
import edu.dosw.styleradar.estadisticas.domain.model.TipoSerie;
import edu.dosw.styleradar.estadisticas.domain.port.in.ConsultarGraficaUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GraficaRestControllerNegativeTest {

    private MockMvc mockMvc;
    private ExplodingConsultarGraficaUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ExplodingConsultarGraficaUseCase();
        TableroDtoMapper mapper = new TableroDtoMapper();
        GraficaRestController controller = new GraficaRestController(useCase, mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Caso negativo: Fechas invertidas retorna 400 Bad Request")
    void testFechasInvertidasRetorna400() throws Exception {
        UUID id = UUID.randomUUID();
        useCase.exceptionToThrow = new InvalidPeriodoException("La fecha de inicio no puede ser posterior a la fecha de fin");

        mockMvc.perform(get("/estadisticas/graficas/{id}/series", id)
                        .param("fechaInicio", "2026-11-10")
                        .param("fechaFin", "2026-11-01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("La fecha de inicio no puede ser posterior a la fecha de fin"));
    }

    @Test
    @DisplayName("Caso negativo: UUID inválido en la URL retorna 400 Bad Request")
    void testUUIDInvalidoRetorna400() throws Exception {
        mockMvc.perform(get("/estadisticas/graficas/uuid-invalido/series")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Caso negativo: Tipo de serie inválido en query param retorna 400 Bad Request")
    void testTipoSerieInvalidoRetorna400() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(get("/estadisticas/graficas/{id}/series", id)
                        .param("tipo", "ANUAL_INEXISTENTE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Caso negativo: Gráfica no encontrada retorna 404 Not Found")
    void testGraficaNoEncontradaRetorna404() throws Exception {
        UUID id = UUID.randomUUID();
        useCase.exceptionToThrow = new GraficaNotFoundException("Gráfica no encontrada para el identificador: " + id);

        mockMvc.perform(get("/estadisticas/graficas/{id}/series", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Gráfica no encontrada para el identificador: " + id));
    }

    @Test
    @DisplayName("Caso negativo: Error no controlado retorna 500 Internal Server Error")
    void testErrorInesperadoRetorna500() throws Exception {
        UUID id = UUID.randomUUID();
        useCase.exceptionToThrow = new RuntimeException("Falla inesperada en BD");

        mockMvc.perform(get("/estadisticas/graficas/{id}/series", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Error interno inesperado: Falla inesperada en BD"));
    }

    static class ExplodingConsultarGraficaUseCase implements ConsultarGraficaUseCase {
        RuntimeException exceptionToThrow;

        @Override
        public Grafica consultarGrafica(UUID id, TipoSerie tipo, LocalDate fechaInicio, LocalDate fechaFin) {
            if (exceptionToThrow != null) throw exceptionToThrow;
            return null;
        }

        @Override
        public List<Serie> consultarSeries(UUID id, TipoSerie tipo, LocalDate fechaInicio, LocalDate fechaFin) {
            if (exceptionToThrow != null) throw exceptionToThrow;
            return null;
        }

        @Override
        public Grafica consultarActividadDiariaTienda(UUID idDueño, LocalDate fechaInicio, LocalDate fechaFin) {
            if (exceptionToThrow != null) throw exceptionToThrow;
            return null;
        }
    }
}
