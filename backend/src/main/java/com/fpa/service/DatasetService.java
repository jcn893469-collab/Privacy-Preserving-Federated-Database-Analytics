package com.fpa.service;

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

        if (datasetMetadataRepository.findByDatabaseIdAndDatasetName(databaseId, datasetName.trim()).isPresent()) {
            throw new IllegalArgumentException("Dataset with name '" + datasetName + "' already exists in this database");
        }

        DatasetMetadata dataset = DatasetMetadata.builder()
                .database(db)
                .datasetName(datasetName.trim())
                .description(description)
                .totalRecords(totalRecords != null ? totalRecords : 0)
                .schemaVersion("1.0")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return datasetMetadataRepository.save(dataset);
    }

    @Transactional
    public DatasetColumnSchema registerColumn(Long datasetId, String columnName, String dataType,
                                             BigDecimal clippingLower, BigDecimal clippingUpper) {
        User user = currentUserService.requireUser();
        DatasetMetadata dataset = datasetMetadataRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found with id: " + datasetId));

        if (user.getRole() != User.Role.ADMIN && !dataset.getDatabase().getOwner().getId().equals(user.getId())) {
            throw new SecurityException("You do not have permission to modify this dataset schema");
        }

        if (clippingLower.compareTo(clippingUpper) >= 0) {
            throw new IllegalArgumentException("clippingLower must be strictly less than clippingUpper");
        }

        BigDecimal sensitivity = clippingUpper.subtract(clippingLower);

        DatasetColumnSchema column = DatasetColumnSchema.builder()
                .dataset(dataset)
                .columnName(columnName.trim())
                .dataType(dataType != null ? dataType : "NUMERIC")
                .clippingLower(clippingLower)
                .clippingUpper(clippingUpper)
                .sensitivity(sensitivity)
                .targetMetric(true)
                .build();

        return columnSchemaRepository.save(column);
    }

    @Transactional(readOnly = true)
    public List<DatasetMetadata> getDatasetsForDatabase(Long databaseId) {
        return datasetMetadataRepository.findByDatabaseId(databaseId);
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
}
