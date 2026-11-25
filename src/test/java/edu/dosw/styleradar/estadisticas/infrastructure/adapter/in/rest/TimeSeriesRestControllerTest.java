package edu.dosw.styleradar.estadisticas.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.estadisticas.application.dto.DtoMapper;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Period;
import edu.dosw.styleradar.estadisticas.domain.model.Point;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.model.TimeGranularity;
import edu.dosw.styleradar.estadisticas.domain.model.TimeSeries;
import edu.dosw.styleradar.estadisticas.domain.port.in.QueryTimeSeriesUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.exception.GlobalExceptionHandler;
import edu.dosw.styleradar.estadisticas.infrastructure.security.AuthenticatedScopeResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TimeSeriesRestControllerTest {

    private final QueryTimeSeriesUseCase useCase = mock(QueryTimeSeriesUseCase.class);
    private final AuthenticatedScopeResolver scopeResolver = mock(AuthenticatedScopeResolver.class);
    private MockMvc mockMvc;
    private Scope scope;
    private Period period;

    @BeforeEach
    void setUp() {
        scope = new Scope("platform", ScopeType.PLATFORM);
        period = new Period(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-02"));
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new TimeSeriesRestController(useCase, scopeResolver, new DtoMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void resolvesScopeQueriesUseCaseAndReturnsTimeSeriesDto() throws Exception {
        when(scopeResolver.resolve(MetricCode.PLATFORM_USER_REGISTRATIONS)).thenReturn(scope);
        when(useCase.query(
                eq(MetricCode.PLATFORM_USER_REGISTRATIONS),
                eq(scope),
                eq(period),
                eq(TimeGranularity.DAY)))
                .thenReturn(new TimeSeries(
                        MetricCode.PLATFORM_USER_REGISTRATIONS,
                        scope,
                        period,
                        TimeGranularity.DAY,
                        List.of(
                                new Point(period.startDate(), BigDecimal.ZERO),
                                new Point(period.endDate(), BigDecimal.valueOf(3)))));

        mockMvc.perform(get("/statistics/time-series/PLATFORM_USER_REGISTRATIONS")
                        .param("startDate", "2026-11-01")
                        .param("endDate", "2026-11-02")
                        .param("granularity", "DAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricCode").value("PLATFORM_USER_REGISTRATIONS"))
                .andExpect(jsonPath("$.scopeId").value("platform"))
                .andExpect(jsonPath("$.granularity").value("DAY"))
                .andExpect(jsonPath("$.points.length()").value(2))
                .andExpect(jsonPath("$.points[0].value").value(0))
                .andExpect(jsonPath("$.points[1].value").value(3));

        verify(scopeResolver).resolve(MetricCode.PLATFORM_USER_REGISTRATIONS);
        verify(useCase).query(MetricCode.PLATFORM_USER_REGISTRATIONS, scope, period, TimeGranularity.DAY);
    }

    @Test
    void rejectsMismatchedStartAndEndDateParametersAndSupportsNoDates() throws Exception {
        mockMvc.perform(get("/statistics/time-series/PLATFORM_USER_REGISTRATIONS")
                        .param("startDate", "2026-11-01"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/statistics/time-series/PLATFORM_USER_REGISTRATIONS")
                        .param("endDate", "2026-11-02"))
                .andExpect(status().isBadRequest());

        when(scopeResolver.resolve(MetricCode.PLATFORM_SUBSCRIPTION_REVENUE)).thenReturn(scope);
        when(useCase.query(MetricCode.PLATFORM_SUBSCRIPTION_REVENUE, scope, null, null))
                .thenReturn(new TimeSeries(
                        MetricCode.PLATFORM_SUBSCRIPTION_REVENUE,
                        scope,
                        period,
                        TimeGranularity.MONTH,
                        List.of()));

        mockMvc.perform(get("/statistics/time-series/PLATFORM_SUBSCRIPTION_REVENUE"))
                .andExpect(status().isOk());
    }
}
