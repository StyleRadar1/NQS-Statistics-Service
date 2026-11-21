package edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "metric_item_daily",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_metric_item_daily_scope_metric_item_day",
                columnNames = {"scope_type", "scope_id", "metric_code", "category_code", "item_id", "day"}),
        indexes = @Index(
                name = "idx_metric_item_daily_scope_metric",
                columnList = "scope_type, scope_id, metric_code"))
public class MetricItemDailyEntity {

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

    @Column(name = "item_id", nullable = false, length = 100)
    private String itemId;

    @Column(name = "item_name", nullable = false, length = 160)
    private String itemName;

    @Column(name = "day", nullable = false)
    private LocalDate day;

    @Column(name = "value", nullable = false)
    private long value;

    protected MetricItemDailyEntity() {
    }

    public MetricItemDailyEntity(
            UUID id,
            String scopeType,
            String scopeId,
            String metricCode,
            String categoryCode,
            String itemId,
            String itemName,
            LocalDate day,
            long value
    ) {
        this.id = id;
        this.scopeType = scopeType;
        this.scopeId = scopeId;
        this.metricCode = metricCode;
        this.categoryCode = categoryCode;
        this.itemId = itemId;
        this.itemName = itemName;
        this.day = day;
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    public void clearValue() {
        this.value = 0L;
    }

    public void adjustValue(long delta) {
        this.value = Math.max(0L, this.value + delta);
    }

    public void updateItemName(String itemName) {
        if (itemName != null && !itemName.isBlank()) {
            this.itemName = itemName;
        }
    }
}
