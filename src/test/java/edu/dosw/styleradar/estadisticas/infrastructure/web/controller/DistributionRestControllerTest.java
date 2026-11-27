package edu.dosw.styleradar.estadisticas.infrastructure.web.controller;

import edu.dosw.styleradar.estadisticas.application.dto.DtoMapper;
import edu.dosw.styleradar.estadisticas.domain.model.CategoryCode;
import edu.dosw.styleradar.estadisticas.domain.model.Distribution;
import edu.dosw.styleradar.estadisticas.domain.model.DistributionEntry;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.port.in.QueryDistributionUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.security.AuthenticatedScopeResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DistributionRestControllerTest {

    private final QueryDistributionUseCase useCase = mock(QueryDistributionUseCase.class);
    private final AuthenticatedScopeResolver scopeResolver = mock(AuthenticatedScopeResolver.class);
    private final Scope scope = new Scope("platform", ScopeType.PLATFORM);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new DistributionRestController(useCase, scopeResolver, new DtoMapper())).build();
    }

    @Test
    void resolvesAuthenticatedScopeAndReturnsDistributionResponse() throws Exception {
        when(scopeResolver.resolve(MetricCode.PLATFORM_BUYERS_BY_PLAN)).thenReturn(scope);
        when(useCase.query(MetricCode.PLATFORM_BUYERS_BY_PLAN, scope)).thenReturn(new Distribution(
                MetricCode.PLATFORM_BUYERS_BY_PLAN,
                scope,
                List.of(new DistributionEntry(
                        CategoryCode.PLAN_RADAR,
                        8,
                        new BigDecimal("100.0")))));

        mockMvc.perform(get("/statistics/distributions/PLATFORM_BUYERS_BY_PLAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricCode").value("PLATFORM_BUYERS_BY_PLAN"))
                .andExpect(jsonPath("$.scopeId").value("platform"))
                .andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.entries[0].categoryCode").value("PLAN_RADAR"))
                .andExpect(jsonPath("$.entries[0].count").value(8))
                .andExpect(jsonPath("$.entries[0].percentage").value(100.0));

        verify(scopeResolver).resolve(MetricCode.PLATFORM_BUYERS_BY_PLAN);
        verify(useCase).query(MetricCode.PLATFORM_BUYERS_BY_PLAN, scope);
    }
}
