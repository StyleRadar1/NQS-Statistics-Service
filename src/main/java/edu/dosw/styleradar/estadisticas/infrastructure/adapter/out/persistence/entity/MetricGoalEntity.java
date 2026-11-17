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
        name = "metric_goal",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_metric_goal_scope_metric_period",
                columnNames = {"scope_type", "scope_id", "metric_code", "period_start_date"}))
public class MetricGoalEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "scope_type", nullable = false, length = 30)
    private String scopeType;

    @Column(name = "scope_id", nullable = false, length = 100)
    private String scopeId;

    @Column(name = "metric_code", nullable = false, length = 100)
    private String metricCode;

    @Column(name = "period_start_date", nullable = false)
    private LocalDate periodStartDate;

    @Column(name = "target_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal targetValue;

    protected MetricGoalEntity() {
    }

    public MetricGoalEntity(
            UUID id,
            String scopeType,
            String scopeId,
            String metricCode,
            LocalDate periodStartDate,
            BigDecimal targetValue
    ) {
        this.id = id;
        this.scopeType = scopeType;
        this.scopeId = scopeId;
        this.metricCode = metricCode;
        this.periodStartDate = periodStartDate;
        this.targetValue = targetValue;
    }

    public BigDecimal getTargetValue() {
        return targetValue;
    }
}
