package edu.dosw.styleradar.estadisticas.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.estadisticas.application.dto.DtoMapper;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.RealTimeCount;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.port.in.QueryRealTimeCountUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.security.AuthenticatedScopeResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RealTimeCountRestControllerTest {

    private final QueryRealTimeCountUseCase useCase = mock(QueryRealTimeCountUseCase.class);
    private final AuthenticatedScopeResolver scopeResolver = mock(AuthenticatedScopeResolver.class);
    private final Scope scope = new Scope("store-17", ScopeType.STORE);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new RealTimeCountRestController(useCase, scopeResolver, new DtoMapper())).build();
    }

    @Test
    void resolvesAuthenticatedScopeAndReturnsCountDto() throws Exception {
        when(scopeResolver.resolve(MetricCode.STORE_REALTIME_VISITORS)).thenReturn(scope);
        when(useCase.query(MetricCode.STORE_REALTIME_VISITORS, scope))
                .thenReturn(new RealTimeCount(MetricCode.STORE_REALTIME_VISITORS, scope, 6));

        mockMvc.perform(get("/statistics/realtime/STORE_REALTIME_VISITORS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricCode").value("STORE_REALTIME_VISITORS"))
                .andExpect(jsonPath("$.scopeType").value("STORE"))
                .andExpect(jsonPath("$.scopeId").value("store-17"))
                .andExpect(jsonPath("$.activeCount").value(6));

        verify(scopeResolver).resolve(MetricCode.STORE_REALTIME_VISITORS);
        verify(useCase).query(MetricCode.STORE_REALTIME_VISITORS, scope);
    }
}
