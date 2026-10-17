package com.fpa.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "dataset_column_schema")
public class DatasetColumnSchema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id", nullable = false)
    private DatasetMetadata dataset;

    @Column(name = "column_name", nullable = false, length = 100)
    private String columnName;

    @Column(name = "data_type", nullable = false, length = 50)
    private String dataType = "NUMERIC";

    @Column(name = "clipping_lower", nullable = false, precision = 18, scale = 4)
    private BigDecimal clippingLower = BigDecimal.ZERO;

    @Column(name = "clipping_upper", nullable = false, precision = 18, scale = 4)
    private BigDecimal clippingUpper = BigDecimal.valueOf(100.0);

    @Column(name = "sensitivity", nullable = false, precision = 18, scale = 4)
    private BigDecimal sensitivity = BigDecimal.valueOf(100.0);

    @Column(name = "is_target_metric", nullable = false)
    private boolean targetMetric = true;

    public DatasetColumnSchema() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DatasetMetadata getDataset() { return dataset; }
    public void setDataset(DatasetMetadata dataset) { this.dataset = dataset; }
    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public BigDecimal getClippingLower() { return clippingLower; }
    public void setClippingLower(BigDecimal clippingLower) { this.clippingLower = clippingLower; }
    public BigDecimal getClippingUpper() { return clippingUpper; }
    public void setClippingUpper(BigDecimal clippingUpper) { this.clippingUpper = clippingUpper; }
    public BigDecimal getSensitivity() { return sensitivity; }
    public void setSensitivity(BigDecimal sensitivity) { this.sensitivity = sensitivity; }
    public boolean isTargetMetric() { return targetMetric; }
    public void setTargetMetric(boolean targetMetric) { this.targetMetric = targetMetric; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final DatasetColumnSchema target = new DatasetColumnSchema();

        public Builder dataset(DatasetMetadata dataset) { target.dataset = dataset; return this; }
        public Builder columnName(String columnName) { target.columnName = columnName; return this; }
        public Builder dataType(String dataType) { target.dataType = dataType; return this; }
        public Builder clippingLower(BigDecimal clippingLower) { target.clippingLower = clippingLower; return this; }
        public Builder clippingUpper(BigDecimal clippingUpper) { target.clippingUpper = clippingUpper; return this; }
        public Builder sensitivity(BigDecimal sensitivity) { target.sensitivity = sensitivity; return this; }
        public Builder targetMetric(boolean targetMetric) { target.targetMetric = targetMetric; return this; }
        public DatasetColumnSchema build() { return target; }
    }
}
