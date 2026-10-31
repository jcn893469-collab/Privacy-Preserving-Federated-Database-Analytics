package com.fpa.service;

import com.fpa.api.dto.DatasetColumnRequest;
import com.fpa.api.dto.DatasetUpdateRequest;
import com.fpa.domain.entity.DatasetColumnSchema;
import com.fpa.domain.entity.DatasetMetadata;
import com.fpa.domain.entity.FederatedDatabase;
import com.fpa.domain.entity.User;
import com.fpa.domain.repository.DatasetColumnSchemaRepository;
import com.fpa.domain.repository.DatasetMetadataRepository;
import com.fpa.domain.repository.FederatedDatabaseRepository;
import com.fpa.security.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DatasetService {

    private final DatasetMetadataRepository datasetMetadataRepository;
    private final DatasetColumnSchemaRepository columnSchemaRepository;
    private final FederatedDatabaseRepository databaseRepository;
    private final CurrentUserService currentUserService;

    public DatasetService(DatasetMetadataRepository datasetMetadataRepository,
                          DatasetColumnSchemaRepository columnSchemaRepository,
                          FederatedDatabaseRepository databaseRepository,
                          CurrentUserService currentUserService) {
        this.datasetMetadataRepository = datasetMetadataRepository;
        this.columnSchemaRepository = columnSchemaRepository;
        this.databaseRepository = databaseRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public DatasetMetadata registerDataset(Long databaseId, String datasetName, String description, Integer totalRecords) {
        User user = currentUserService.requireUser();
        FederatedDatabase db = databaseRepository.findById(databaseId)
                .orElseThrow(() -> new IllegalArgumentException("Database not found with id: " + databaseId));

        if (user.getRole() != User.Role.ADMIN && !db.getOwner().getId().equals(user.getId())) {
            throw new SecurityException("You do not have permission to add datasets to this database");
        }

        String normalizedName = requireText(datasetName, "datasetName");
        if (datasetMetadataRepository.findByDatabaseIdAndDatasetName(databaseId, normalizedName).isPresent()) {
            throw new IllegalArgumentException("Dataset with name '" + datasetName + "' already exists in this database");
        }

        DatasetMetadata dataset = DatasetMetadata.builder()
                .database(db)
                .datasetName(normalizedName)
                .description(description)
                .totalRecords(totalRecords != null ? totalRecords : 0)
                .schemaVersion("1.0")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return datasetMetadataRepository.save(dataset);
    }

    @Transactional
    public DatasetColumnSchema registerColumn(Long datasetId, DatasetColumnRequest request) {
        User user = currentUserService.requireUser();
        DatasetMetadata dataset = datasetMetadataRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found with id: " + datasetId));

        verifyDatasetOwner(dataset, user, "modify this dataset schema");
        return columnSchemaRepository.save(buildColumn(dataset, request));
    }

    @Transactional
    public DatasetColumnSchema updateColumn(Long datasetId, Long columnId, DatasetColumnRequest request) {
        User user = currentUserService.requireUser();
        DatasetMetadata dataset = datasetMetadataRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found with id: " + datasetId));
        verifyDatasetOwner(dataset, user, "modify this dataset schema");

        DatasetColumnSchema column = columnSchemaRepository.findById(columnId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset column not found with id: " + columnId));
        if (!column.getDataset().getId().equals(datasetId)) {
            throw new IllegalArgumentException("Dataset column does not belong to dataset: " + datasetId);
        }
        String normalizedName = requireText(request.getColumnName(), "columnName");
        columnSchemaRepository.findByDatasetIdAndColumnName(datasetId, normalizedName)
                .filter(existing -> !existing.getId().equals(columnId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Column with name '" + normalizedName + "' already exists in this dataset");
                });
        applyColumnRequest(column, request);
        return columnSchemaRepository.save(column);
    }

    @Transactional(readOnly = true)
    public List<DatasetMetadata> getDatasetsForDatabase(Long databaseId) {
        return datasetMetadataRepository.findByDatabaseId(databaseId);
    }

    @Transactional(readOnly = true)
    public DatasetMetadata getDataset(Long datasetId) {
        return datasetMetadataRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found with id: " + datasetId));
    }

    @Transactional
    public DatasetMetadata updateDataset(Long datasetId, DatasetUpdateRequest request) {
        User user = currentUserService.requireUser();
        DatasetMetadata dataset = getDataset(datasetId);
        verifyDatasetOwner(dataset, user, "update this dataset");
        String normalizedName = requireText(request.getDatasetName(), "datasetName");
        datasetMetadataRepository.findByDatabaseIdAndDatasetName(dataset.getDatabase().getId(), normalizedName)
                .filter(existing -> !existing.getId().equals(datasetId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Dataset with name '" + normalizedName + "' already exists in this database");
                });
        dataset.setDatasetName(normalizedName);
        dataset.setDescription(request.getDescription());
        dataset.setTotalRecords(request.getTotalRecords());
        dataset.setSchemaVersion(requireText(request.getSchemaVersion(), "schemaVersion"));
        dataset.setUpdatedAt(LocalDateTime.now());
        return datasetMetadataRepository.save(dataset);
    }

    @Transactional(readOnly = true)
    public Page<DatasetMetadata> searchDatasets(Long databaseId, String query, Pageable pageable) {
        return datasetMetadataRepository.searchAndFilter(databaseId, query, pageable);
    }

    @Transactional(readOnly = true)
    public List<DatasetColumnSchema> getColumnsForDataset(Long datasetId) {
        return columnSchemaRepository.findByDatasetId(datasetId);
    }

    @Transactional(readOnly = true)
    public void verifyDatasetAccess(Long datasetId, User caller) {
        DatasetMetadata dataset = datasetMetadataRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found with id: " + datasetId));
        if (caller.getRole() != User.Role.ADMIN
                && !dataset.getDatabase().getOwner().getId().equals(caller.getId())) {
            throw new SecurityException("You do not have permission to view this dataset");
        }
    }

    @Transactional
    public void deleteDataset(Long datasetId) {
        User user = currentUserService.requireUser();
        DatasetMetadata dataset = datasetMetadataRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found with id: " + datasetId));

        if (user.getRole() != User.Role.ADMIN && !dataset.getDatabase().getOwner().getId().equals(user.getId())) {
            throw new SecurityException("You do not have permission to delete this dataset");
        }
        datasetMetadataRepository.delete(dataset);
    }

    @Transactional
    public void deleteColumn(Long datasetId, Long columnId) {
        User user = currentUserService.requireUser();
        DatasetMetadata dataset = datasetMetadataRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found with id: " + datasetId));
        verifyDatasetOwner(dataset, user, "delete this dataset schema");
        DatasetColumnSchema column = columnSchemaRepository.findById(columnId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset column not found with id: " + columnId));
        if (!column.getDataset().getId().equals(datasetId)) {
            throw new IllegalArgumentException("Dataset column does not belong to dataset: " + datasetId);
        }
        columnSchemaRepository.delete(column);
    }

    private DatasetColumnSchema buildColumn(DatasetMetadata dataset, DatasetColumnRequest request) {
        String normalizedName = requireText(request.getColumnName(), "columnName");
        if (columnSchemaRepository.findByDatasetIdAndColumnName(dataset.getId(), normalizedName).isPresent()) {
            throw new IllegalArgumentException("Column with name '" + normalizedName + "' already exists in this dataset");
        }
        DatasetColumnSchema column = DatasetColumnSchema.builder().dataset(dataset).build();
        applyColumnRequest(column, request);
        return column;
    }

    private void applyColumnRequest(DatasetColumnSchema column, DatasetColumnRequest request) {
        BigDecimal lower = request.getClippingLower();
        BigDecimal upper = request.getClippingUpper();
        if (lower == null || upper == null || lower.compareTo(upper) >= 0) {
            throw new IllegalArgumentException("clippingLower must be strictly less than clippingUpper");
        }
        column.setColumnName(requireText(request.getColumnName(), "columnName"));
        column.setDataType(requireText(request.getDataType(), "dataType"));
        column.setClippingLower(lower);
        column.setClippingUpper(upper);
        column.setSensitivity(upper.subtract(lower));
        column.setTargetMetric(request.getTargetMetric() == null || request.getTargetMetric());
    }

    private void verifyDatasetOwner(DatasetMetadata dataset, User user, String action) {
        if (user.getRole() != User.Role.ADMIN && !dataset.getDatabase().getOwner().getId().equals(user.getId())) {
            throw new SecurityException("You do not have permission to " + action);
        }
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
