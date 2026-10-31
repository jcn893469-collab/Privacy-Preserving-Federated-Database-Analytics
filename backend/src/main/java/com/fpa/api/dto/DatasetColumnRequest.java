package com.fpa.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public class DatasetColumnRequest {

    @NotBlank
    @Size(max = 100)
    private String columnName;

    @NotBlank
    @Size(max = 50)
    private String dataType = "NUMERIC";

    @NotNull
    @Digits(integer = 14, fraction = 4)
    private BigDecimal clippingLower;

    @NotNull
    @Digits(integer = 14, fraction = 4)
    private BigDecimal clippingUpper;

    private Boolean targetMetric = true;

    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public BigDecimal getClippingLower() { return clippingLower; }
    public void setClippingLower(BigDecimal clippingLower) { this.clippingLower = clippingLower; }
    public BigDecimal getClippingUpper() { return clippingUpper; }
    public void setClippingUpper(BigDecimal clippingUpper) { this.clippingUpper = clippingUpper; }
    public Boolean getTargetMetric() { return targetMetric; }
    public void setTargetMetric(Boolean targetMetric) { this.targetMetric = targetMetric; }
}
