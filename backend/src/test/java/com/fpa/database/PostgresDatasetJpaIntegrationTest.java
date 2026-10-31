package com.fpa.database;

import com.fpa.domain.entity.DatasetColumnSchema;
import com.fpa.domain.entity.DatasetMetadata;
import com.fpa.domain.entity.FederatedDatabase;
import com.fpa.domain.entity.User;
import com.fpa.domain.repository.DatasetColumnSchemaRepository;
import com.fpa.domain.repository.DatasetMetadataRepository;
import com.fpa.domain.repository.FederatedDatabaseRepository;
import com.fpa.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DataJpaTest(properties = {
    "spring.flyway.locations=classpath:db/migration/common,classpath:db/migration/dev",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class PostgresDatasetJpaIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FederatedDatabaseRepository databaseRepository;

    @Autowired
    private DatasetMetadataRepository datasetRepository;

    @Autowired
    private DatasetColumnSchemaRepository columnRepository;

    @Test
    void datasetAndColumnCrudSearchAndPaginationUseTheFlywaySchema() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User owner = userRepository.save(User.builder()
            .fullName("Dataset Integration Owner")
            .email("dataset-" + suffix + "@integration.test")
            .passwordHash("test-hash")
            .role(User.Role.DATA_OWNER)
            .active(true)
            .createdAt(LocalDateTime.now())
            .build());
        FederatedDatabase database = databaseRepository.save(FederatedDatabase.builder()
            .organizationName("Dataset Integration Organization")
            .dbIdentifier("dataset-integration-" + suffix)
            .owner(owner)
            .active(true)
            .registeredAt(LocalDateTime.now())
            .build());

        DatasetMetadata dataset = datasetRepository.save(newDataset(database, "Vitals Primary"));
        datasetRepository.save(newDataset(database, "Vitals Secondary"));
        datasetRepository.save(newDataset(database, "Unrelated"));

        Page<DatasetMetadata> results = datasetRepository.searchAndFilter(
            database.getId(),
            "vitals",
            PageRequest.of(0, 1)
        );
        assertEquals(1, results.getContent().size());
        assertEquals(2, results.getTotalElements());

        dataset.setDescription("Updated description");
        dataset.setTotalRecords(42);
        dataset.setSchemaVersion("2.0");
        dataset.setUpdatedAt(LocalDateTime.now());
        datasetRepository.save(dataset);
        assertEquals("Updated description", datasetRepository.findById(dataset.getId()).orElseThrow().getDescription());

        DatasetColumnSchema column = columnRepository.save(newColumn(dataset, "systolic"));
        column.setColumnName("systolic_pressure");
        column.setClippingUpper(new BigDecimal("180.0000"));
        column.setSensitivity(new BigDecimal("110.0000"));
        columnRepository.save(column);
        assertEquals("systolic_pressure",
            columnRepository.findById(column.getId()).orElseThrow().getColumnName());

        columnRepository.deleteById(column.getId());
        assertFalse(columnRepository.findById(column.getId()).isPresent());

        DatasetColumnSchema cascadingColumn = columnRepository.save(newColumn(dataset, "diastolic"));
        datasetRepository.deleteById(dataset.getId());
        assertFalse(datasetRepository.findById(dataset.getId()).isPresent());
        assertFalse(columnRepository.findById(cascadingColumn.getId()).isPresent());
    }

    private DatasetMetadata newDataset(FederatedDatabase database, String name) {
        return DatasetMetadata.builder()
            .database(database)
            .datasetName(name)
            .description("Integration dataset")
            .schemaVersion("1.0")
            .totalRecords(10)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    private DatasetColumnSchema newColumn(DatasetMetadata dataset, String name) {
        return DatasetColumnSchema.builder()
            .dataset(dataset)
            .columnName(name)
            .dataType("NUMERIC")
            .clippingLower(new BigDecimal("70.0000"))
            .clippingUpper(new BigDecimal("200.0000"))
            .sensitivity(new BigDecimal("130.0000"))
            .targetMetric(true)
            .build();
    }
}
