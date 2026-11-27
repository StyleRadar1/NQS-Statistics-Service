package edu.dosw.styleradar.estadisticas.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.estadisticas.application.dto.DtoMapper;
import edu.dosw.styleradar.estadisticas.application.dto.TimeSeriesResponseDto;
import edu.dosw.styleradar.estadisticas.domain.exception.InvalidTimeSeriesRequestException;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Period;
import edu.dosw.styleradar.estadisticas.domain.model.TimeGranularity;
import edu.dosw.styleradar.estadisticas.domain.port.in.QueryTimeSeriesUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.security.AuthenticatedScopeResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/statistics/time-series")
@Tag(name = "Time Series", description = "Daily user registrations and monthly subscription revenue")
public class TimeSeriesRestController {

    private final QueryTimeSeriesUseCase queryTimeSeriesUseCase;
    private final AuthenticatedScopeResolver scopeResolver;
    private final DtoMapper dtoMapper;

    public TimeSeriesRestController(
            QueryTimeSeriesUseCase queryTimeSeriesUseCase,
            AuthenticatedScopeResolver scopeResolver,
            DtoMapper dtoMapper
    ) {
        this.queryTimeSeriesUseCase = queryTimeSeriesUseCase;
        this.scopeResolver = scopeResolver;
        this.dtoMapper = dtoMapper;
    }

    @GetMapping("/{metricCode}")
    @Operation(
            summary = "Query a daily or monthly time series",
            description = "Intervals without data are returned with a value of zero. "
                    + "PLATFORM_USER_REGISTRATIONS defaults to the current month (DAY granularity); "
                    + "PLATFORM_SUBSCRIPTION_REVENUE defaults to the last six calendar months (MONTH granularity).")
    public ResponseEntity<TimeSeriesResponseDto> query(
            @Parameter(
                    description = "PLATFORM_USER_REGISTRATIONS (daily user registrations) or "
                            + "PLATFORM_SUBSCRIPTION_REVENUE (monthly subscription revenue)",
                    required = true,
                    schema = @Schema(allowableValues = {
                            "PLATFORM_USER_REGISTRATIONS",
                            "PLATFORM_SUBSCRIPTION_REVENUE"}))
            @PathVariable MetricCode metricCode,
            @Parameter(description = "Inclusive start date (ISO: YYYY-MM-DD)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Inclusive end date (ISO: YYYY-MM-DD)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "DAY or MONTH; defaults to the granularity required by the metric")
            @RequestParam(required = false) TimeGranularity granularity
    ) {
        if ((startDate == null) != (endDate == null)) {
            throw new InvalidTimeSeriesRequestException("startDate and endDate must be provided together");
        }

        Period dateRange = startDate == null ? null : new Period(startDate, endDate);
        var scope = scopeResolver.resolve(metricCode);
        var result = queryTimeSeriesUseCase.query(metricCode, scope, dateRange, granularity);
        return ResponseEntity.ok(dtoMapper.toDto(result));
    }
}
