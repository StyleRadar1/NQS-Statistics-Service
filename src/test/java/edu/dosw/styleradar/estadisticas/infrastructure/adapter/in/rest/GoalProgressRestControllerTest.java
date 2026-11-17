package edu.dosw.styleradar.estadisticas.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.estadisticas.application.dto.DtoMapper;
import edu.dosw.styleradar.estadisticas.domain.model.GoalProgress;
import edu.dosw.styleradar.estadisticas.domain.model.GoalProgressStatus;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Period;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.port.in.QueryGoalProgressUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.security.AuthenticatedScopeResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GoalProgressRestControllerTest {

    private final QueryGoalProgressUseCase useCase = mock(QueryGoalProgressUseCase.class);
    private final AuthenticatedScopeResolver scopeResolver = mock(AuthenticatedScopeResolver.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new GoalProgressRestController(useCase, scopeResolver, new DtoMapper())).build();
    }

    @Test
    void resolvesActorScopeAndReturnsCurrentValueWhenGoalIsNotDefined() throws Exception {
        Scope scope = new Scope("foundation-1", ScopeType.FOUNDATION);
        when(scopeResolver.resolve(MetricCode.FOUNDATION_GARMENTS_RECEIVED)).thenReturn(scope);
        when(useCase.query(MetricCode.FOUNDATION_GARMENTS_RECEIVED, scope)).thenReturn(new GoalProgress(
                MetricCode.FOUNDATION_GARMENTS_RECEIVED,
                scope,
                new Period(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-17")),
                BigDecimal.valueOf(25),
                null,
                null,
                GoalProgressStatus.GOAL_NOT_DEFINED));

        mockMvc.perform(get("/api/v1/statistics/goals/FOUNDATION_GARMENTS_RECEIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricCode").value("FOUNDATION_GARMENTS_RECEIVED"))
                .andExpect(jsonPath("$.scopeId").value("foundation-1"))
                .andExpect(jsonPath("$.currentValue").value(25))
                .andExpect(jsonPath("$.targetValue").doesNotExist())
                .andExpect(jsonPath("$.progressPercentage").doesNotExist())
                .andExpect(jsonPath("$.status").value("GOAL_NOT_DEFINED"));

        verify(scopeResolver).resolve(MetricCode.FOUNDATION_GARMENTS_RECEIVED);
        verify(useCase).query(MetricCode.FOUNDATION_GARMENTS_RECEIVED, scope);
    }
}
