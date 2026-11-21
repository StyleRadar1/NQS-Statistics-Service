package edu.dosw.styleradar.estadisticas.infrastructure.web.controller;

import edu.dosw.styleradar.estadisticas.application.dto.DtoMapper;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.RankedItem;
import edu.dosw.styleradar.estadisticas.domain.model.Ranking;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.port.in.QueryRankingUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.security.AuthenticatedScopeResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RankingRestControllerTest {

    private final QueryRankingUseCase useCase = mock(QueryRankingUseCase.class);
    private final AuthenticatedScopeResolver scopeResolver = mock(AuthenticatedScopeResolver.class);
    private final Scope scope = new Scope("store-17", ScopeType.STORE);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new RankingRestController(useCase, scopeResolver, new DtoMapper())).build();
    }

    @Test
    void resolvesAuthenticatedScopeAndReturnsRankingResponse() throws Exception {
        when(scopeResolver.resolve(MetricCode.STORE_GARMENTS_BY_SAVES)).thenReturn(scope);
        when(useCase.query(MetricCode.STORE_GARMENTS_BY_SAVES, scope)).thenReturn(new Ranking(
                MetricCode.STORE_GARMENTS_BY_SAVES,
                scope,
                6,
                List.of(new RankedItem("garment-9", "Jacket", 1, 25)),
                null));

        mockMvc.perform(get("/statistics/rankings/STORE_GARMENTS_BY_SAVES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricCode").value("STORE_GARMENTS_BY_SAVES"))
                .andExpect(jsonPath("$.scopeId").value("store-17"))
                .andExpect(jsonPath("$.limit").value(6))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].itemId").value("garment-9"))
                .andExpect(jsonPath("$.items[0].savesCount").value(25));

        verify(scopeResolver).resolve(MetricCode.STORE_GARMENTS_BY_SAVES);
        verify(useCase).query(MetricCode.STORE_GARMENTS_BY_SAVES, scope);
    }

    @Test
    void returnsTheEmptyCatalogMessageWhenThereAreNoItems() throws Exception {
        when(scopeResolver.resolve(MetricCode.STORE_GARMENTS_BY_SAVES)).thenReturn(scope);
        when(useCase.query(MetricCode.STORE_GARMENTS_BY_SAVES, scope)).thenReturn(new Ranking(
                MetricCode.STORE_GARMENTS_BY_SAVES,
                scope,
                6,
                List.of(),
                "No hay prendas publicadas"));

        mockMvc.perform(get("/statistics/rankings/STORE_GARMENTS_BY_SAVES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.message").value("No hay prendas publicadas"));
    }
}
