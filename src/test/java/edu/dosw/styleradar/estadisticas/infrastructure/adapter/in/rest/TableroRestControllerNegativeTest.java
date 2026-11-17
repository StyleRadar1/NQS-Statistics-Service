package edu.dosw.styleradar.estadisticas.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.estadisticas.application.mapper.TableroDtoMapper;
import edu.dosw.styleradar.estadisticas.domain.exception.InvalidPeriodoException;
import edu.dosw.styleradar.estadisticas.domain.exception.TableroNotFoundException;
import edu.dosw.styleradar.estadisticas.domain.model.Tablero;
import edu.dosw.styleradar.estadisticas.domain.model.TipoTablero;
import edu.dosw.styleradar.estadisticas.domain.port.in.ConsultarTableroUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TableroRestControllerNegativeTest {

    private MockMvc mockMvc;
    private ExplodingConsultarTableroUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ExplodingConsultarTableroUseCase();
        TableroDtoMapper mapper = new TableroDtoMapper();
        TableroRestController controller = new TableroRestController(useCase, mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Caso negativo: Fechas invertidas (fechaInicio posterior a fechaFin) retorna 400 Bad Request")
    void testFechasInvertidasRetorna400() throws Exception {
        UUID idDueño = UUID.randomUUID();
        useCase.exceptionToThrow = new InvalidPeriodoException("La fecha de inicio no puede ser posterior a la fecha de fin");

        mockMvc.perform(get("/estadisticas/tableros/TIENDA/{idDueño}", idDueño)
                        .param("fechaInicio", "2026-10-31")
                        .param("fechaFin", "2026-10-01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("La fecha de inicio no puede ser posterior a la fecha de fin"));
    }

    @Test
    @DisplayName("Caso negativo: Tipo de tablero inválido en la URL retorna 400 Bad Request")
    void testTipoInvalidoRetorna400() throws Exception {
        UUID idDueño = UUID.randomUUID();

        mockMvc.perform(get("/estadisticas/tableros/TIPO_INEXISTENTE/{idDueño}", idDueño)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Caso negativo: UUID inválido en la URL retorna 400 Bad Request")
    void testUUIDInvalidoRetorna400() throws Exception {
        mockMvc.perform(get("/estadisticas/tableros/TIENDA/uuid-no-valido")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Caso negativo: Tablero no encontrado retorna 404 Not Found")
    void testTableroNoEncontradoRetorna404() throws Exception {
        UUID idDueño = UUID.randomUUID();
        useCase.exceptionToThrow = new TableroNotFoundException("Tablero no encontrado para el id proporcionado");

        mockMvc.perform(get("/estadisticas/tableros/TIENDA/{idDueño}", idDueño)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Tablero no encontrado para el id proporcionado"));
    }

    @Test
    @DisplayName("Caso negativo: Error no controlado retorna 500 Internal Server Error")
    void testErrorInesperadoRetorna500() throws Exception {
        UUID idDueño = UUID.randomUUID();
        useCase.exceptionToThrow = new RuntimeException("Error en base de datos");

        mockMvc.perform(get("/estadisticas/tableros/TIENDA/{idDueño}", idDueño)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Error interno inesperado: Error en base de datos"));
    }

    static class ExplodingConsultarTableroUseCase implements ConsultarTableroUseCase {
        RuntimeException exceptionToThrow;

        @Override
        public Tablero consultarTablero(TipoTablero tipo, UUID idDueño, LocalDate fechaInicio, LocalDate fechaFin) {
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return null;
        }
    }
}
