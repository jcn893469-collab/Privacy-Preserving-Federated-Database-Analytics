package com.fpa.api.dto;

import com.fpa.domain.entity.DatasetColumnSchema;

import java.math.BigDecimal;

public record DatasetColumnResponse(
    Long id,
    Long datasetId,
    String columnName,
    String dataType,
    BigDecimal clippingLower,
    BigDecimal clippingUpper,
    BigDecimal sensitivity,
    boolean targetMetric
) {
    public static DatasetColumnResponse from(DatasetColumnSchema column) {
        return new DatasetColumnResponse(
            column.getId(),
            column.getDataset().getId(),
            column.getColumnName(),
            column.getDataType(),
            column.getClippingLower(),
            column.getClippingUpper(),
            column.getSensitivity(),
            column.isTargetMetric()
        );
    }
}
