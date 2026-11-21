package edu.dosw.styleradar.estadisticas.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.estadisticas.application.dto.DtoMapper;
import edu.dosw.styleradar.estadisticas.domain.model.ComparisonWindow;
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
        scope = new Scope("store-17", ScopeType.STORE);
        period = new Period(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-02"));
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new TimeSeriesRestController(useCase, scopeResolver, new DtoMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void resolvesScopeQueriesUseCaseAndReturnsTimeSeriesDto() throws Exception {
        when(scopeResolver.resolve(MetricCode.STORE_PAGE_VISITS)).thenReturn(scope);
        when(useCase.query(
                eq(MetricCode.STORE_PAGE_VISITS),
                eq(scope),
                eq(period),
                eq(TimeGranularity.DAY)))
                .thenReturn(new TimeSeries(
                        MetricCode.STORE_PAGE_VISITS,
                        scope,
                        period,
                        TimeGranularity.DAY,
                        List.of(
                                new Point(period.startDate(), BigDecimal.ZERO),
                                new Point(period.endDate(), BigDecimal.valueOf(3)))));

        mockMvc.perform(get("/statistics/time-series/STORE_PAGE_VISITS")
                        .param("startDate", "2026-11-01")
                        .param("endDate", "2026-11-02")
                        .param("granularity", "DAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricCode").value("STORE_PAGE_VISITS"))
                .andExpect(jsonPath("$.scopeId").value("store-17"))
                .andExpect(jsonPath("$.granularity").value("DAY"))
                .andExpect(jsonPath("$.points.length()").value(2))
                .andExpect(jsonPath("$.points[0].value").value(0))
                .andExpect(jsonPath("$.points[1].value").value(3));

        verify(scopeResolver).resolve(MetricCode.STORE_PAGE_VISITS);
        verify(useCase).query(MetricCode.STORE_PAGE_VISITS, scope, period, TimeGranularity.DAY);
    }

    @Test
    void supportsPeriodoParameterAndRejectsCombiningPeriodoWithExplicitDates() throws Exception {
        when(scopeResolver.resolve(MetricCode.STORE_MESSAGES_RECEIVED)).thenReturn(scope);
        when(useCase.queryByWindow(MetricCode.STORE_MESSAGES_RECEIVED, scope, ComparisonWindow.DIAS_7))
                .thenReturn(new TimeSeries(
                        MetricCode.STORE_MESSAGES_RECEIVED,
                        scope,
                        period,
                        TimeGranularity.DAY,
                        List.of(new Point(period.startDate(), BigDecimal.ONE))));

        mockMvc.perform(get("/statistics/time-series/STORE_MESSAGES_RECEIVED")
                        .param("periodo", "7_DIAS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricCode").value("STORE_MESSAGES_RECEIVED"))
                .andExpect(jsonPath("$.points[0].value").value(1));

        mockMvc.perform(get("/statistics/time-series/STORE_MESSAGES_RECEIVED")
                        .param("periodo", "7_DIAS")
                        .param("startDate", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PERIOD"));

        verify(useCase).queryByWindow(MetricCode.STORE_MESSAGES_RECEIVED, scope, ComparisonWindow.DIAS_7);
    }

    @Test
    void rejectsMismatchedStartAndEndDateParametersAndSupportsNoDates() throws Exception {
        mockMvc.perform(get("/statistics/time-series/STORE_PAGE_VISITS")
                        .param("startDate", "2026-11-01"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/statistics/time-series/STORE_PAGE_VISITS")
                        .param("endDate", "2026-11-02"))
                .andExpect(status().isBadRequest());

        when(scopeResolver.resolve(MetricCode.STORE_PAGE_VISITS)).thenReturn(scope);
        when(useCase.query(MetricCode.STORE_PAGE_VISITS, scope, null, null))
                .thenReturn(new TimeSeries(
                        MetricCode.STORE_PAGE_VISITS,
                        scope,
                        period,
                        TimeGranularity.DAY,
                        List.of()));

        mockMvc.perform(get("/statistics/time-series/STORE_PAGE_VISITS"))
                .andExpect(status().isOk());
    }

    @Test
    void returnsUnifiedSixMonthDonationRequestsForAuthenticatedFoundation() throws Exception {
        Scope foundationScope = new Scope("foundation-7", ScopeType.FOUNDATION);
        Period sixMonths = new Period(LocalDate.parse("2026-06-01"), LocalDate.parse("2026-11-30"));
        when(scopeResolver.resolve(MetricCode.FOUNDATION_REQUESTS_FROM_USERS)).thenReturn(foundationScope);
        when(useCase.query(MetricCode.FOUNDATION_REQUESTS_FROM_USERS, foundationScope, null, TimeGranularity.MONTH))
                .thenReturn(new TimeSeries(
                        MetricCode.FOUNDATION_REQUESTS_FROM_USERS,
                        foundationScope,
                        sixMonths,
                        TimeGranularity.MONTH,
                        List.of(
                                new Point(LocalDate.parse("2026-06-01"), BigDecimal.ZERO),
                                new Point(LocalDate.parse("2026-07-01"), BigDecimal.valueOf(2)),
                                new Point(LocalDate.parse("2026-08-01"), BigDecimal.ZERO),
                                new Point(LocalDate.parse("2026-09-01"), BigDecimal.valueOf(4)),
                                new Point(LocalDate.parse("2026-10-01"), BigDecimal.ZERO),
                                new Point(LocalDate.parse("2026-11-01"), BigDecimal.valueOf(6)))));
        when(useCase.query(MetricCode.FOUNDATION_REQUESTS_FROM_STORES, foundationScope, null, TimeGranularity.MONTH))
                .thenReturn(new TimeSeries(
                        MetricCode.FOUNDATION_REQUESTS_FROM_STORES,
                        foundationScope,
                        sixMonths,
                        TimeGranularity.MONTH,
                        List.of(
                                new Point(LocalDate.parse("2026-06-01"), BigDecimal.ONE),
                                new Point(LocalDate.parse("2026-07-01"), BigDecimal.ZERO),
                                new Point(LocalDate.parse("2026-08-01"), BigDecimal.valueOf(3)),
                                new Point(LocalDate.parse("2026-09-01"), BigDecimal.ZERO),
                                new Point(LocalDate.parse("2026-10-01"), BigDecimal.valueOf(5)),
                                new Point(LocalDate.parse("2026-11-01"), BigDecimal.ZERO))));

        mockMvc.perform(get("/statistics/time-series/foundations/monthly-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.foundationId").value("foundation-7"))
                .andExpect(jsonPath("$.solicitudesPersonas.length()").value(6))
                .andExpect(jsonPath("$.solicitudesPersonas[0].mes").value("2026-06"))
                .andExpect(jsonPath("$.solicitudesPersonas[0].cantidad").value(0))
                .andExpect(jsonPath("$.solicitudesPersonas[5].mes").value("2026-11"))
                .andExpect(jsonPath("$.solicitudesPersonas[5].cantidad").value(6))
                .andExpect(jsonPath("$.solicitudesTiendas.length()").value(6))
                .andExpect(jsonPath("$.solicitudesTiendas[0].cantidad").value(1))
                .andExpect(jsonPath("$.solicitudesTiendas[4].mes").value("2026-10"))
                .andExpect(jsonPath("$.solicitudesTiendas[4].cantidad").value(5));
    }
}
