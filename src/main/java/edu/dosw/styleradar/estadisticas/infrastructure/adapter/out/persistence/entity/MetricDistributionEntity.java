package edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "metric_distribution")
public class MetricDistributionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "scope_type", nullable = false, length = 30)
    private String scopeType;

    @Column(name = "scope_id", nullable = false, length = 100)
    private String scopeId;

    @Column(name = "metric_code", nullable = false, length = 100)
    private String metricCode;

    @Column(name = "category_code", nullable = false, length = 100)
    private String categoryCode;

    @Column(name = "value", nullable = false)
    private long value;

    protected MetricDistributionEntity() {
    }

    public MetricDistributionEntity(
            UUID id,
            String scopeType,
            String scopeId,
            String metricCode,
            String categoryCode,
            long value
    ) {
        this.id = id;
        this.scopeType = scopeType;
        this.scopeId = scopeId;
        this.metricCode = metricCode;
        this.categoryCode = categoryCode;
        this.value = value;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public long getValue() {
        return value;
    }
}
