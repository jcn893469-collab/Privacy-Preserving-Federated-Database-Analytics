package com.fpa.api.dto;

import com.fpa.domain.entity.DatasetMetadata;

import java.time.LocalDateTime;

public record DatasetResponse(
    Long id,
    Long databaseId,
    String datasetName,
    String description,
    String schemaVersion,
    Integer totalRecords,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static DatasetResponse from(DatasetMetadata dataset) {
        return new DatasetResponse(
            dataset.getId(),
            dataset.getDatabase().getId(),
            dataset.getDatasetName(),
            dataset.getDescription(),
            dataset.getSchemaVersion(),
            dataset.getTotalRecords(),
            dataset.getCreatedAt(),
            dataset.getUpdatedAt()
        );
    }
}
