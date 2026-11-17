package edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "metric_daily",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_metric_daily_scope_metric_date",
                columnNames = {"scope_type", "scope_id", "metric_code", "metric_date"}))
public class MetricDailyEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "scope_type", nullable = false, length = 30)
    private String scopeType;

    @Column(name = "scope_id", nullable = false, length = 100)
    private String scopeId;

    @Column(name = "metric_code", nullable = false, length = 100)
    private String metricCode;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(name = "value", nullable = false, precision = 18, scale = 2)
    private BigDecimal value;

    protected MetricDailyEntity() {
    }

    public MetricDailyEntity(
            UUID id,
            String scopeType,
            String scopeId,
            String metricCode,
            LocalDate metricDate,
            BigDecimal value
    ) {
        this.id = id;
        this.scopeType = scopeType;
        this.scopeId = scopeId;
        this.metricCode = metricCode;
        this.metricDate = metricDate;
        this.value = value;
    }

    public UUID getId() {
        return id;
    }

    public String getScopeType() {
        return scopeType;
    }

    public String getScopeId() {
        return scopeId;
    }

    public String getMetricCode() {
        return metricCode;
    }

    public LocalDate getMetricDate() {
        return metricDate;
    }

    public BigDecimal getValue() {
        return value;
    }
}
