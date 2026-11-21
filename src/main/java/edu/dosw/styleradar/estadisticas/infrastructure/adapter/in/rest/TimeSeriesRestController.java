package edu.dosw.styleradar.estadisticas.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.estadisticas.application.dto.DtoMapper;
import edu.dosw.styleradar.estadisticas.application.dto.TimeSeriesResponseDto;
import edu.dosw.styleradar.estadisticas.application.dto.response.MonthlyDonationRequestsResponseDto;
import edu.dosw.styleradar.estadisticas.domain.exception.InvalidTimeSeriesRequestException;
import edu.dosw.styleradar.estadisticas.domain.model.ComparisonWindow;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Period;
import edu.dosw.styleradar.estadisticas.domain.model.TimeGranularity;
import edu.dosw.styleradar.estadisticas.domain.port.in.QueryTimeSeriesUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.security.AuthenticatedScopeResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "Time Series", description = "Daily and monthly metric time series")
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

    @GetMapping("/foundations/monthly-requests")
    @Operation(
            summary = "Query donation requests for the last 6 months separated by origin (SR-RF-83)",
            description = "Returns the monthly series for the last six calendar months for requests originating from "
                    + "individuals and stores. Months without requests are returned with zero values.")
    public ResponseEntity<MonthlyDonationRequestsResponseDto> queryFoundationMonthlyRequests() {
        var scope = scopeResolver.resolve(MetricCode.FOUNDATION_REQUESTS_FROM_USERS);
        var fromUsers = queryTimeSeriesUseCase.query(
                MetricCode.FOUNDATION_REQUESTS_FROM_USERS, scope, null, TimeGranularity.MONTH);
        var fromStores = queryTimeSeriesUseCase.query(
                MetricCode.FOUNDATION_REQUESTS_FROM_STORES, scope, null, TimeGranularity.MONTH);
        return ResponseEntity.ok(dtoMapper.toMonthlyDonationRequestsDto(scope.id(), fromUsers, fromStores));
    }

    @GetMapping("/{metricCode}")
    @Operation(
            summary = "Query a daily or monthly time series",
            description = "Intervals without data are returned with a value of zero. "
                    + "For daily store metrics, it accepts the period parameter (7_DAYS, 30_DAYS, 90_DAYS). "
                    + "Monthly metrics for foundation requests and subscription revenue without a date range "
                    + "cover the last six calendar months.")
    public ResponseEntity<TimeSeriesResponseDto> query(
            @Parameter(description = "Supported metric code", required = true)
            @PathVariable MetricCode metricCode,
            @Parameter(description = "Predefined window for daily store metrics: 7_DAYS, 30_DAYS, or 90_DAYS (also accepts 7_DIAS, 30_DIAS, 90_DIAS)")
            @RequestParam(name = "period", required = false) String period,
            @Parameter(hidden = true)
            @RequestParam(name = "periodo", required = false) String periodo,
            @Parameter(description = "Inclusive start date (ISO: YYYY-MM-DD); optional for revenue (defaults to last 6 months)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Inclusive end date (ISO: YYYY-MM-DD); optional for revenue (defaults to last 6 months)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "DAY or MONTH; defaults to the granularity required by the metric")
            @RequestParam(required = false) TimeGranularity granularity
    ) {
        String rawPeriod = (period != null && !period.isBlank()) ? period : periodo;
        if (rawPeriod != null && !rawPeriod.isBlank()) {
            if (startDate != null || endDate != null) {
                throw new InvalidTimeSeriesRequestException(
                        "No se puede combinar el parámetro periodo con startDate o endDate");
            }
            ComparisonWindow window = ComparisonWindow.fromCode(rawPeriod);
            var scope = scopeResolver.resolve(metricCode);
            var result = queryTimeSeriesUseCase.queryByWindow(metricCode, scope, window);
            return ResponseEntity.ok(dtoMapper.toDto(result));
        }

        if ((startDate == null) != (endDate == null)) {
            throw new InvalidTimeSeriesRequestException("startDate and endDate must be provided together");
        }

        Period dateRange = startDate == null ? null : new Period(startDate, endDate);
        var scope = scopeResolver.resolve(metricCode);
        var result = queryTimeSeriesUseCase.query(metricCode, scope, dateRange, granularity);
        return ResponseEntity.ok(dtoMapper.toDto(result));
    }
}
