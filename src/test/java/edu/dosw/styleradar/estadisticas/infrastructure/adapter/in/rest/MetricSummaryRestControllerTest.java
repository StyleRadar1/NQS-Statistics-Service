package edu.dosw.styleradar.estadisticas.infrastructure.adapter.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.dosw.styleradar.estadisticas.application.dto.DtoMapper;
import edu.dosw.styleradar.estadisticas.domain.model.ComparisonWindow;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.MetricSummaryWithVariation;
import edu.dosw.styleradar.estadisticas.domain.model.MetricTotal;
import edu.dosw.styleradar.estadisticas.domain.model.Period;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.model.VariationStatus;
import edu.dosw.styleradar.estadisticas.domain.port.in.QueryMetricSummaryUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.exception.GlobalExceptionHandler;
import edu.dosw.styleradar.estadisticas.infrastructure.security.AuthenticatedScopeResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
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

class MetricSummaryRestControllerTest {

    private final QueryMetricSummaryUseCase useCase = mock(QueryMetricSummaryUseCase.class);
    private final AuthenticatedScopeResolver scopeResolver = mock(AuthenticatedScopeResolver.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new MetricSummaryRestController(useCase, scopeResolver, new DtoMapper()))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsSummaryWithVariation() throws Exception {
        Scope scope = new Scope("store-17", ScopeType.STORE);
        when(scopeResolver.resolve(MetricCode.STORE_PAGE_VISITS)).thenReturn(scope);
        when(useCase.queryWithVariation(MetricCode.STORE_PAGE_VISITS, scope, ComparisonWindow.DIAS_7))
                .thenReturn(new MetricSummaryWithVariation(
                        MetricCode.STORE_PAGE_VISITS,
                        scope,
                        ComparisonWindow.DIAS_7,
                        new Period(LocalDate.parse("2026-11-15"), LocalDate.parse("2026-11-21")),
                        new Period(LocalDate.parse("2026-11-08"), LocalDate.parse("2026-11-14")),
                        BigDecimal.valueOf(120),
                        BigDecimal.valueOf(80),
                        new BigDecimal("50.00"),
                        VariationStatus.CALCULATED,
                        null));

        mockMvc.perform(get("/statistics/summary/STORE_PAGE_VISITS").param("periodo", "7_DIAS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricCode").value("STORE_PAGE_VISITS"))
                .andExpect(jsonPath("$.periodo").value("7_DIAS"))
                .andExpect(jsonPath("$.total").value(120))
                .andExpect(jsonPath("$.previousTotal").value(80))
                .andExpect(jsonPath("$.variationPercentage").value(50.0))
                .andExpect(jsonPath("$.variationStatus").value("CALCULATED"));

        verify(useCase).queryWithVariation(MetricCode.STORE_PAGE_VISITS, scope, ComparisonWindow.DIAS_7);
    }

    @Test
    void rejectsInvalidOrMissingPeriodoWithBadRequest() throws Exception {
        mockMvc.perform(get("/statistics/summary/STORE_PAGE_VISITS").param("periodo", "15_DIAS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PERIOD"));

        mockMvc.perform(get("/statistics/summary/STORE_PAGE_VISITS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"));
    }

    @Test
    void returnsAllTimeAndMonthlyTotals() throws Exception {
        Scope foundationScope = new Scope("foundation-1", ScopeType.FOUNDATION);
        when(scopeResolver.resolve(MetricCode.FOUNDATION_GARMENTS_RECEIVED)).thenReturn(foundationScope);
        when(useCase.queryTotal(MetricCode.FOUNDATION_GARMENTS_RECEIVED, foundationScope))
                .thenReturn(new MetricTotal(
                        MetricCode.FOUNDATION_GARMENTS_RECEIVED,
                        foundationScope,
                        null,
                        BigDecimal.valueOf(240)));

        mockMvc.perform(get("/statistics/summary/totals/FOUNDATION_GARMENTS_RECEIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricCode").value("FOUNDATION_GARMENTS_RECEIVED"))
                .andExpect(jsonPath("$.scopeId").value("foundation-1"))
                .andExpect(jsonPath("$.period").doesNotExist())
                .andExpect(jsonPath("$.total").value(240));

        Scope platformScope = new Scope("platform", ScopeType.PLATFORM);
        Period month = new Period(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30"));
        when(scopeResolver.resolve(MetricCode.PLATFORM_MONTHLY_DONATIONS)).thenReturn(platformScope);
        when(useCase.queryTotal(MetricCode.PLATFORM_MONTHLY_DONATIONS, platformScope))
                .thenReturn(new MetricTotal(
                        MetricCode.PLATFORM_MONTHLY_DONATIONS,
                        platformScope,
                        month,
                        BigDecimal.valueOf(45)));

        mockMvc.perform(get("/statistics/summary/totals/PLATFORM_MONTHLY_DONATIONS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricCode").value("PLATFORM_MONTHLY_DONATIONS"))
                .andExpect(jsonPath("$.period.startDate").value("2026-11-01"))
                .andExpect(jsonPath("$.period.endDate").value("2026-11-30"))
                .andExpect(jsonPath("$.total").value(45));
    }
}
