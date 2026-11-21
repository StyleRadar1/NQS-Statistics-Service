package edu.dosw.styleradar.estadisticas.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.estadisticas.application.dto.DtoMapper;
import edu.dosw.styleradar.estadisticas.domain.model.GoalProgress;
import edu.dosw.styleradar.estadisticas.domain.model.GoalProgressStatus;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Period;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.port.in.QueryGoalProgressUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.exception.GlobalExceptionHandler;
import edu.dosw.styleradar.estadisticas.infrastructure.security.AuthenticatedScopeResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GoalProgressRestControllerTest {

    private final QueryGoalProgressUseCase useCase = mock(QueryGoalProgressUseCase.class);
    private final AuthenticatedScopeResolver scopeResolver = mock(AuthenticatedScopeResolver.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new GoalProgressRestController(useCase, scopeResolver, new DtoMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returns422WithCurrentValueAndMessageWhenGoalIsNotDefined() throws Exception {
        Scope scope = new Scope("foundation-1", ScopeType.FOUNDATION);
        when(scopeResolver.resolve(MetricCode.FOUNDATION_GARMENTS_RECEIVED)).thenReturn(scope);
        when(useCase.query(MetricCode.FOUNDATION_GARMENTS_RECEIVED, scope)).thenReturn(new GoalProgress(
                MetricCode.FOUNDATION_GARMENTS_RECEIVED,
                scope,
                new Period(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-17")),
                BigDecimal.valueOf(25),
                null,
                null,
                GoalProgressStatus.GOAL_NOT_DEFINED,
                "Tu fundación aún no tiene una meta mensual de prendas"));

        mockMvc.perform(get("/statistics/goals/FOUNDATION_GARMENTS_RECEIVED"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.metricCode").value("FOUNDATION_GARMENTS_RECEIVED"))
                .andExpect(jsonPath("$.scopeId").value("foundation-1"))
                .andExpect(jsonPath("$.currentValue").value(25))
                .andExpect(jsonPath("$.targetValue").doesNotExist())
                .andExpect(jsonPath("$.progressPercentage").doesNotExist())
                .andExpect(jsonPath("$.status").value("GOAL_NOT_DEFINED"))
                .andExpect(jsonPath("$.message").value("Tu fundación aún no tiene una meta mensual de prendas"));

        verify(scopeResolver).resolve(MetricCode.FOUNDATION_GARMENTS_RECEIVED);
        verify(useCase).query(MetricCode.FOUNDATION_GARMENTS_RECEIVED, scope);
    }

    @Test
    void returns200WhenGoalIsDefinedAndSupportsUpdatingGoal() throws Exception {
        Scope scope = new Scope("store-1", ScopeType.STORE);
        when(scopeResolver.resolve(MetricCode.STORE_PAGE_VISITS)).thenReturn(scope);
        GoalProgress defined = new GoalProgress(
                MetricCode.STORE_PAGE_VISITS,
                scope,
                new Period(LocalDate.parse("2026-11-16"), LocalDate.parse("2026-11-21")),
                BigDecimal.valueOf(150),
                BigDecimal.valueOf(200),
                new BigDecimal("75.0"),
                GoalProgressStatus.GOAL_DEFINED,
                null);
        when(useCase.query(MetricCode.STORE_PAGE_VISITS, scope)).thenReturn(defined);
        when(useCase.defineGoal(MetricCode.STORE_PAGE_VISITS, scope, BigDecimal.valueOf(200))).thenReturn(defined);

        mockMvc.perform(get("/statistics/goals/STORE_PAGE_VISITS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercentage").value(75.0))
                .andExpect(jsonPath("$.status").value("GOAL_DEFINED"));

        mockMvc.perform(put("/statistics/goals/STORE_PAGE_VISITS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetValue\": 200}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetValue").value(200));

        mockMvc.perform(put("/statistics/goals/STORE_PAGE_VISITS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetValue\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATA"));
    }
}
