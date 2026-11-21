package edu.dosw.styleradar.estadisticas.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.estadisticas.domain.exception.UnauthorizedScopeException;
import edu.dosw.styleradar.estadisticas.domain.model.CategoryCode;
import edu.dosw.styleradar.estadisticas.domain.model.ModerationDecision;
import edu.dosw.styleradar.estadisticas.domain.model.ModerationDecisionResult;
import edu.dosw.styleradar.estadisticas.domain.model.PendingModeration;
import edu.dosw.styleradar.estadisticas.domain.model.PendingModerationGroup;
import edu.dosw.styleradar.estadisticas.domain.model.PendingModerationPublisher;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.port.in.GetPendingModerationUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.exception.GlobalExceptionHandler;
import edu.dosw.styleradar.estadisticas.infrastructure.security.AuthenticatedScopeResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PendingModerationRestControllerTest {

    private final GetPendingModerationUseCase useCase = mock(GetPendingModerationUseCase.class);
    private final AuthenticatedScopeResolver scopeResolver = mock(AuthenticatedScopeResolver.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new PendingModerationRestController(useCase, scopeResolver))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsPendingPublishersGroupedByType() throws Exception {
        Scope scope = new Scope("platform", ScopeType.PLATFORM);
        when(scopeResolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn(scope);
        when(useCase.query(scope)).thenReturn(new PendingModeration(
                List.of(
                        new PendingModerationGroup(
                                CategoryCode.ORIGIN_STORE,
                                List.of(new PendingModerationPublisher("store-1", "Store One", 3))),
                        new PendingModerationGroup(
                                CategoryCode.ORIGIN_USER,
                                List.of(new PendingModerationPublisher("user-1", "User One", 2)))),
                5,
                null));

        mockMvc.perform(get("/statistics/moderation/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPendingCount").value(5))
                .andExpect(jsonPath("$.groups[0].publisherType").value("ORIGIN_STORE"))
                .andExpect(jsonPath("$.groups[0].publishers[0].publisherId").value("store-1"))
                .andExpect(jsonPath("$.groups[0].publishers[0].pendingCount").value(3))
                .andExpect(jsonPath("$.groups[1].publisherType").value("ORIGIN_USER"))
                .andExpect(jsonPath("$.groups[1].publishers[0].publisherId").value("user-1"))
                .andExpect(jsonPath("$.groups[1].publishers[0].pendingCount").value(2));
    }

    @Test
    void returnsOkWithEmptyListsAndMessageWhenThereAreNoPendingPublications() throws Exception {
        Scope scope = new Scope("platform", ScopeType.PLATFORM);
        when(scopeResolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn(scope);
        when(useCase.query(scope)).thenReturn(new PendingModeration(
                List.of(),
                0,
                "No hay publicaciones pendientes de moderación"));

        mockMvc.perform(get("/statistics/moderation/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPendingCount").value(0))
                .andExpect(jsonPath("$.groups").isEmpty())
                .andExpect(jsonPath("$.message").value("No hay publicaciones pendientes de moderación"));
    }

    @Test
    void mapsUnauthorizedScopeToForbidden() throws Exception {
        when(scopeResolver.resolve(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new UnauthorizedScopeException("Only platform administrators can query moderation"));

        mockMvc.perform(get("/statistics/moderation/pending"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void approvesAndRejectsModerationDecisionAndRejectsInvalidPublisherType() throws Exception {
        Scope scope = new Scope("platform", ScopeType.PLATFORM);
        when(scopeResolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn(scope);
        when(useCase.decide(scope, CategoryCode.ORIGIN_STORE, "store-1", ModerationDecision.APROBAR))
                .thenReturn(new ModerationDecisionResult(
                        CategoryCode.ORIGIN_STORE,
                        "store-1",
                        "APROBADA",
                        3L,
                        new PendingModeration(
                                List.of(new PendingModerationGroup(
                                        CategoryCode.ORIGIN_USER,
                                        List.of(new PendingModerationPublisher("user-1", "User One", 2)))),
                                2,
                                null)));

        mockMvc.perform(post("/statistics/moderation/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoPublicador": "TIENDA",
                                  "identificador": "store-1",
                                  "decision": "APROBAR",
                                  "motivo": "Cumple politicas"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoPublicador").value("ORIGIN_STORE"))
                .andExpect(jsonPath("$.identificador").value("store-1"))
                .andExpect(jsonPath("$.resultadoModeracion").value("APROBADA"))
                .andExpect(jsonPath("$.moderatedCount").value(3))
                .andExpect(jsonPath("$.pendientesPorGrupo.totalPendingCount").value(2));

        when(useCase.decide(scope, CategoryCode.ORIGIN_USER, "user-1", ModerationDecision.RECHAZAR))
                .thenReturn(new ModerationDecisionResult(
                        CategoryCode.ORIGIN_USER,
                        "user-1",
                        "RECHAZADA",
                        2L,
                        new PendingModeration(List.of(), 0, "No hay publicaciones pendientes de moderación")));

        mockMvc.perform(post("/statistics/moderation/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoPublicador": "USUARIO",
                                  "identificador": "user-1",
                                  "decision": "RECHAZAR"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoModeracion").value("RECHAZADA"))
                .andExpect(jsonPath("$.pendientesPorGrupo.totalPendingCount").value(0));

        mockMvc.perform(post("/statistics/moderation/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoPublicador": "DESCONOCIDO",
                                  "identificador": "store-1",
                                  "decision": "APROBAR"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATA"));
    }
}
