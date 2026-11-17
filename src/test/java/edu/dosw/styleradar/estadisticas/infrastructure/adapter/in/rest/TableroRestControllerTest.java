package edu.dosw.styleradar.estadisticas.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.estadisticas.application.mapper.TableroDtoMapper;
import edu.dosw.styleradar.estadisticas.domain.model.Comparacion;
import edu.dosw.styleradar.estadisticas.domain.model.Indicador;
import edu.dosw.styleradar.estadisticas.domain.model.Periodo;
import edu.dosw.styleradar.estadisticas.domain.model.Tablero;
import edu.dosw.styleradar.estadisticas.domain.model.TipoTablero;
import edu.dosw.styleradar.estadisticas.domain.model.UnidadMedida;
import edu.dosw.styleradar.estadisticas.domain.port.in.ConsultarTableroUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TableroRestControllerTest {

    private MockMvc mockMvc;
    private FakeConsultarTableroUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FakeConsultarTableroUseCase();
        TableroDtoMapper mapper = new TableroDtoMapper();
        TableroRestController controller = new TableroRestController(useCase, mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API · Swagger: GET /estadisticas/tableros/{tipo}/{idDueño} con periodo retorna 200 OK e indicadores")
    void testConsultarTableroConPeriodoExitoso() throws Exception {
        UUID idDueño = UUID.randomUUID();
        UUID tableroId = UUID.randomUUID();
        LocalDate inicio = LocalDate.of(2026, 10, 1);
        LocalDate fin = LocalDate.of(2026, 10, 31);
        Periodo periodo = new Periodo(inicio, fin);

        Tablero tablero = Tablero.builder()
                .id(tableroId)
                .idDueño(idDueño)
                .tipo(TipoTablero.TIENDA)
                .indicadores(List.of(
                        Indicador.builder()
                                .id(UUID.randomUUID())
                                .nombre("Visitas a la página")
                                .valor(BigDecimal.valueOf(150))
                                .unidad(UnidadMedida.VISITAS)
                                .fecha(LocalDateTime.now())
                                .periodo(periodo)
                                .comparacion(new Comparacion(BigDecimal.valueOf(100), BigDecimal.valueOf(50.00)))
                                .build(),
                        Indicador.builder()
                                .id(UUID.randomUUID())
                                .nombre("Mensajes de clientes")
                                .valor(BigDecimal.valueOf(40))
                                .unidad(UnidadMedida.MENSAJES)
                                .fecha(LocalDateTime.now())
                                .periodo(periodo)
                                .comparacion(new Comparacion(BigDecimal.valueOf(50), BigDecimal.valueOf(-20.00)))
                                .build(),
                        Indicador.builder()
                                .id(UUID.randomUUID())
                                .nombre("Prendas guardadas en wishlist")
                                .valor(BigDecimal.valueOf(90))
                                .unidad(UnidadMedida.PRENDAS)
                                .fecha(LocalDateTime.now())
                                .periodo(periodo)
                                .comparacion(new Comparacion(BigDecimal.valueOf(60), BigDecimal.valueOf(50.00)))
                                .build()
                ))
                .build();

        useCase.resultado = tablero;

        mockMvc.perform(get("/estadisticas/tableros/TIENDA/{idDueño}", idDueño)
                        .param("fechaInicio", "2026-10-01")
                        .param("fechaFin", "2026-10-31")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(tableroId.toString()))
                .andExpect(jsonPath("$.idDueño").value(idDueño.toString()))
                .andExpect(jsonPath("$.tipo").value("TIENDA"))
                .andExpect(jsonPath("$.indicadores", hasSize(3)))
                .andExpect(jsonPath("$.indicadores[0].nombre").value("Visitas a la página"))
                .andExpect(jsonPath("$.indicadores[0].valor").value(150))
                .andExpect(jsonPath("$.indicadores[0].comparacion.variacionPorcentual").value(50.0))
                .andExpect(jsonPath("$.indicadores[1].nombre").value("Mensajes de clientes"))
                .andExpect(jsonPath("$.indicadores[1].valor").value(40))
                .andExpect(jsonPath("$.indicadores[1].comparacion.variacionPorcentual").value(-20.0))
                .andExpect(jsonPath("$.indicadores[2].nombre").value("Prendas guardadas en wishlist"))
                .andExpect(jsonPath("$.indicadores[2].valor").value(90))
                .andExpect(jsonPath("$.indicadores[2].comparacion.variacionPorcentual").value(50.0));
    }

    @Test
    @DisplayName("API · Swagger: GET sin parámetros de fecha resuelve periodo por defecto y retorna 200 OK")
    void testConsultarTableroSinFechas() throws Exception {
        UUID idDueño = UUID.randomUUID();
        UUID tableroId = UUID.randomUUID();

        Tablero tablero = Tablero.builder()
                .id(tableroId)
                .idDueño(idDueño)
                .tipo(TipoTablero.TIENDA)
                .indicadores(new ArrayList<>())
                .build();

        useCase.resultado = tablero;

        mockMvc.perform(get("/estadisticas/tableros/TIENDA/{idDueño}", idDueño)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tableroId.toString()))
                .andExpect(jsonPath("$.tipo").value("TIENDA"));
    }

    static class FakeConsultarTableroUseCase implements ConsultarTableroUseCase {
        Tablero resultado;

        @Override
        public Tablero consultarTablero(TipoTablero tipo, UUID idDueño, LocalDate fechaInicio, LocalDate fechaFin) {
            return resultado;
        }
    }
}
