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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatasetServiceTest {

    @Mock
    private DatasetMetadataRepository datasetRepository;

    @Mock
    private DatasetColumnSchemaRepository columnRepository;

    @Mock
    private FederatedDatabaseRepository databaseRepository;

    @Mock
    private CurrentUserService currentUserService;

    private DatasetService datasetService;
    private User owner;
    private FederatedDatabase database;
    private DatasetMetadata dataset;
    private DatasetColumnSchema column;

    @BeforeEach
    void setUp() {
        datasetService = new DatasetService(datasetRepository, columnRepository, databaseRepository, currentUserService);
        owner = User.builder().role(User.Role.DATA_OWNER).active(true).build();
        owner.setId(1L);
        database = FederatedDatabase.builder().owner(owner).build();
        database.setId(10L);
        dataset = DatasetMetadata.builder().database(database).datasetName("Vitals").totalRecords(20).build();
        dataset.setId(20L);
        column = DatasetColumnSchema.builder().dataset(dataset).columnName("pressure").build();
        column.setId(30L);
        lenient().when(currentUserService.requireUser()).thenReturn(owner);
    }

    @Test
    void createsDatasetMetadataForOwnedDatabase() {
        when(databaseRepository.findById(10L)).thenReturn(Optional.of(database));
        when(datasetRepository.findByDatabaseIdAndDatasetName(10L, "New vitals")).thenReturn(Optional.empty());
        when(datasetRepository.save(any(DatasetMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DatasetMetadata created = datasetService.registerDataset(10L, " New vitals ", "Vitals dataset", 15);

        assertEquals("New vitals", created.getDatasetName());
        assertEquals(15, created.getTotalRecords());
        assertEquals("Vitals dataset", created.getDescription());
    }

    @Test
    void updatesDatasetMetadataAndTimestamp() {
        DatasetUpdateRequest request = new DatasetUpdateRequest();
        request.setDatasetName("Updated vitals");
        request.setDescription("Updated metadata");
        request.setTotalRecords(24);
        request.setSchemaVersion("2.0");
        when(datasetRepository.findById(20L)).thenReturn(Optional.of(dataset));
        when(datasetRepository.findByDatabaseIdAndDatasetName(10L, "Updated vitals")).thenReturn(Optional.empty());
        when(datasetRepository.save(any(DatasetMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DatasetMetadata updated = datasetService.updateDataset(20L, request);

        assertEquals("Updated vitals", updated.getDatasetName());
        assertEquals("Updated metadata", updated.getDescription());
        assertEquals(24, updated.getTotalRecords());
        assertEquals("2.0", updated.getSchemaVersion());
        verify(datasetRepository).save(dataset);
    }

    @Test
    void updatesColumnConfigurationAndRecalculatesSensitivity() {
        DatasetColumnRequest request = columnRequest("systolic", "INTEGER", "70", "180", false);
        when(datasetRepository.findById(20L)).thenReturn(Optional.of(dataset));
        when(columnRepository.findById(30L)).thenReturn(Optional.of(column));
        when(columnRepository.findByDatasetIdAndColumnName(20L, "systolic")).thenReturn(Optional.empty());
        when(columnRepository.save(any(DatasetColumnSchema.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DatasetColumnSchema updated = datasetService.updateColumn(20L, 30L, request);

        assertEquals("systolic", updated.getColumnName());
        assertEquals("INTEGER", updated.getDataType());
        assertEquals(new BigDecimal("110"), updated.getSensitivity());
        assertEquals(false, updated.isTargetMetric());
        verify(columnRepository).save(column);
    }

    @Test
    void createsColumnWithSensitivityDerivedFromClippingBounds() {
        when(datasetRepository.findById(20L)).thenReturn(Optional.of(dataset));
        when(columnRepository.findByDatasetIdAndColumnName(20L, "systolic")).thenReturn(Optional.empty());
        when(columnRepository.save(any(DatasetColumnSchema.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DatasetColumnSchema created = datasetService.registerColumn(
            20L, columnRequest("systolic", "NUMERIC", "70", "180", true)
        );

        assertEquals(new BigDecimal("110"), created.getSensitivity());
        assertEquals("systolic", created.getColumnName());
        verify(columnRepository).save(created);
    }

    @Test
    void rejectsColumnMutationWhenColumnBelongsToAnotherDataset() {
        DatasetMetadata otherDataset = DatasetMetadata.builder().database(dataset.getDatabase()).build();
        otherDataset.setId(21L);
        DatasetColumnSchema foreignColumn = DatasetColumnSchema.builder()
            .dataset(otherDataset)
            .columnName("private")
            .build();
        foreignColumn.setId(31L);
        when(datasetRepository.findById(20L)).thenReturn(Optional.of(dataset));
        when(columnRepository.findById(31L)).thenReturn(Optional.of(foreignColumn));

        assertThrows(IllegalArgumentException.class,
            () -> datasetService.updateColumn(20L, 31L, columnRequest("renamed", "NUMERIC", "0", "10", true)));
    }

    @Test
    void deletesDatasetAndColumnThroughTheirRepositories() {
        when(datasetRepository.findById(20L)).thenReturn(Optional.of(dataset));
        when(columnRepository.findById(30L)).thenReturn(Optional.of(column));

        datasetService.deleteColumn(20L, 30L);
        datasetService.deleteDataset(20L);

        verify(columnRepository).delete(column);
        verify(datasetRepository).delete(dataset);
    }

    @Test
    void normalizesOptionalDatasetSearchTermsToTypedTextValues() {
        when(datasetRepository.searchAndFilter(eq(10L), eq(""), any())).thenReturn(Page.empty());
        when(datasetRepository.searchAndFilter(eq(10L), eq("vitals"), any())).thenReturn(Page.empty());

        datasetService.searchDatasets(10L, null, PageRequest.of(0, 10));
        datasetService.searchDatasets(10L, "   ", PageRequest.of(0, 10));
        datasetService.searchDatasets(10L, " vitals ", PageRequest.of(0, 10));

        verify(datasetRepository, times(2)).searchAndFilter(eq(10L), eq(""), any());
        verify(datasetRepository).searchAndFilter(eq(10L), eq("vitals"), any());
    }

    private DatasetColumnRequest columnRequest(
            String name, String type, String lower, String upper, boolean targetMetric) {
        DatasetColumnRequest request = new DatasetColumnRequest();
        request.setColumnName(name);
        request.setDataType(type);
        request.setClippingLower(new BigDecimal(lower));
        request.setClippingUpper(new BigDecimal(upper));
        request.setTargetMetric(targetMetric);
        return request;
    }
}
