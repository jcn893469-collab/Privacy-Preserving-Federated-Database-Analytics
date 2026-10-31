package com.fpa.database;

import com.fpa.domain.entity.DatasetColumnSchema;
import com.fpa.domain.entity.DatasetMetadata;
import com.fpa.domain.entity.FederatedDatabase;
import com.fpa.domain.entity.User;
import com.fpa.domain.repository.DatasetColumnSchemaRepository;
import com.fpa.domain.repository.DatasetMetadataRepository;
import com.fpa.domain.repository.FederatedDatabaseRepository;
import com.fpa.domain.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DataJpaTest(properties = {
    "spring.flyway.locations=classpath:db/migration/common,classpath:db/migration/dev",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIf("postgresDatabaseAvailable")
class PostgresDatasetJpaIntegrationTest {

    private static final String LOCAL_TEST_DATABASE_URL =
        System.getenv("FPA_TEST_POSTGRES_URL");
    private static final String LOCAL_TEST_DATABASE_USERNAME =
        System.getenv("FPA_TEST_POSTGRES_USERNAME");
    private static final String LOCAL_TEST_DATABASE_PASSWORD =
        System.getenv("FPA_TEST_POSTGRES_PASSWORD");
    private static final Pattern ALLOWED_LOCAL_DATABASE_URL = Pattern.compile(
        "^jdbc:postgresql://(?:localhost|127\\.0\\.0\\.1|\\[::1\\])(?::\\d+)?/fpa_search_test(?:\\?.*)?$",
        Pattern.CASE_INSENSITIVE
    );

    private static PostgreSQLContainer<?> postgresContainer;

    private static boolean hasLocalTestDatabaseUrl() {
        return LOCAL_TEST_DATABASE_URL != null && !LOCAL_TEST_DATABASE_URL.isBlank();
    }

    static boolean postgresDatabaseAvailable() {
        if (hasLocalTestDatabaseUrl()) {
            return true;
        }
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @AfterAll
    static void stopPostgresContainer() {
        if (postgresContainer != null) {
            postgresContainer.stop();
        }
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        if (hasLocalTestDatabaseUrl()) {
            if (!ALLOWED_LOCAL_DATABASE_URL.matcher(LOCAL_TEST_DATABASE_URL).matches()) {
                throw new IllegalStateException(
                    "FPA_TEST_POSTGRES_URL must point to local database fpa_search_test; production hosts are not allowed"
                );
            }
            if (LOCAL_TEST_DATABASE_USERNAME == null || LOCAL_TEST_DATABASE_USERNAME.isBlank()
                    || LOCAL_TEST_DATABASE_PASSWORD == null || LOCAL_TEST_DATABASE_PASSWORD.isBlank()) {
                throw new IllegalStateException(
                    "Set FPA_TEST_POSTGRES_USERNAME and FPA_TEST_POSTGRES_PASSWORD for the local test database"
                );
            }
            registry.add("spring.datasource.url", () -> LOCAL_TEST_DATABASE_URL);
            registry.add("spring.datasource.username", () -> LOCAL_TEST_DATABASE_USERNAME);
            registry.add("spring.datasource.password", () -> LOCAL_TEST_DATABASE_PASSWORD);
            return;
        }

        postgresContainer = new PostgreSQLContainer<>("postgres:16-alpine");
        postgresContainer.start();
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FederatedDatabaseRepository databaseRepository;

    @Autowired
    private DatasetMetadataRepository datasetRepository;

    @Autowired
    private DatasetColumnSchemaRepository columnRepository;

    @Autowired
    private EntityManager entityManager;

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

        FederatedDatabase otherDatabase = databaseRepository.save(FederatedDatabase.builder()
            .organizationName("Other Integration Organization")
            .dbIdentifier("dataset-integration-other-" + suffix)
            .owner(owner)
            .active(true)
            .registeredAt(LocalDateTime.now())
            .build());
        datasetRepository.save(newDataset(otherDatabase, "Vitals External"));

        Page<DatasetMetadata> nullQueryResults = datasetRepository.searchAndFilter(
            database.getId(),
            null,
            PageRequest.of(0, 10)
        );
        assertEquals(3, nullQueryResults.getTotalElements());

        Page<DatasetMetadata> emptyQueryResults = datasetRepository.searchAndFilter(
            database.getId(),
            "",
            PageRequest.of(0, 10)
        );
        assertEquals(3, emptyQueryResults.getTotalElements());

        Page<DatasetMetadata> results = datasetRepository.searchAndFilter(
            database.getId(),
            "vitals",
            PageRequest.of(0, 1)
        );
        assertEquals(1, results.getContent().size());
        assertEquals(2, results.getTotalElements());
        assertEquals(2, datasetRepository.searchAndFilter(database.getId(), "vitals", PageRequest.of(1, 1))
            .getTotalElements());
        assertEquals("Vitals External", datasetRepository.searchAndFilter(
            otherDatabase.getId(), null, PageRequest.of(0, 10)).getContent().get(0).getDatasetName());

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
        entityManager.flush();
        entityManager.clear();
        assertFalse(datasetRepository.findById(dataset.getId()).isPresent());
        assertFalse(columnRepository.findById(cascadingColumn.getId()).isPresent());
    }

    @Test
    void repositorySearchHandlesOptionalTermsDatabaseFilteringPaginationAndSorting() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User owner = createOwner(suffix);
        FederatedDatabase database = createDatabase(owner, "search-" + suffix);
        FederatedDatabase otherDatabase = createDatabase(owner, "search-other-" + suffix);

        LocalDateTime baseTime = LocalDateTime.of(2025, 1, 1, 12, 0);
        datasetRepository.save(newDataset(database, "Vitals Older", baseTime));
        datasetRepository.save(newDataset(database, "Other Dataset", baseTime.plusMinutes(1)));
        datasetRepository.save(newDataset(database, "Vitals Newer", baseTime.plusMinutes(2)));
        datasetRepository.save(newDataset(otherDatabase, "Vitals External", baseTime.plusMinutes(3)));

        assertSearchMatchesAllDatasets(database.getId(), null);
        assertSearchMatchesAllDatasets(database.getId(), "");
        assertSearchMatchesAllDatasets(database.getId(), "   ");

        Page<DatasetMetadata> matching = datasetRepository.searchAndFilter(
            database.getId(), "vitals", PageRequest.of(0, 10)
        );
        assertEquals(2, matching.getTotalElements());
        assertEquals(
            List.of("Vitals Newer", "Vitals Older"),
            matching.getContent().stream().map(DatasetMetadata::getDatasetName).toList()
        );

        Page<DatasetMetadata> otherDatabaseResults = datasetRepository.searchAndFilter(
            otherDatabase.getId(), null, PageRequest.of(0, 10)
        );
        assertEquals(1, otherDatabaseResults.getTotalElements());
        assertEquals("Vitals External", otherDatabaseResults.getContent().get(0).getDatasetName());

        Page<DatasetMetadata> firstPage = datasetRepository.searchAndFilter(
            database.getId(), null, PageRequest.of(0, 1)
        );
        Page<DatasetMetadata> secondPage = datasetRepository.searchAndFilter(
            database.getId(), null, PageRequest.of(1, 1)
        );
        Page<DatasetMetadata> thirdPage = datasetRepository.searchAndFilter(
            database.getId(), null, PageRequest.of(2, 1)
        );
        assertEquals(3, firstPage.getTotalElements());
        assertEquals(3, firstPage.getTotalPages());
        assertEquals("Vitals Newer", firstPage.getContent().get(0).getDatasetName());
        assertEquals("Other Dataset", secondPage.getContent().get(0).getDatasetName());
        assertEquals("Vitals Older", thirdPage.getContent().get(0).getDatasetName());
    }

    private void assertSearchMatchesAllDatasets(Long databaseId, String query) {
        Page<DatasetMetadata> results = datasetRepository.searchAndFilter(
            databaseId, query, PageRequest.of(0, 10)
        );
        assertEquals(3, results.getTotalElements());
        assertEquals(
            List.of("Vitals Newer", "Other Dataset", "Vitals Older"),
            results.getContent().stream().map(DatasetMetadata::getDatasetName).toList()
        );
    }

    private User createOwner(String suffix) {
        return userRepository.save(User.builder()
            .fullName("Dataset Search Test Owner")
            .email("dataset-search-" + suffix + "@integration.test")
            .passwordHash("test-hash")
            .role(User.Role.DATA_OWNER)
            .active(true)
            .createdAt(LocalDateTime.now())
            .build());
    }

    private FederatedDatabase createDatabase(User owner, String identifier) {
        return databaseRepository.save(FederatedDatabase.builder()
            .organizationName("Dataset Search Test Organization")
            .dbIdentifier(identifier)
            .owner(owner)
            .active(true)
            .registeredAt(LocalDateTime.now())
            .build());
    }

    private DatasetMetadata newDataset(FederatedDatabase database, String name) {
        return newDataset(database, name, LocalDateTime.now());
    }

    private DatasetMetadata newDataset(
            FederatedDatabase database, String name, LocalDateTime createdAt) {
        return DatasetMetadata.builder()
            .database(database)
            .datasetName(name)
            .description("Integration dataset")
            .schemaVersion("1.0")
            .totalRecords(10)
            .createdAt(createdAt)
            .updatedAt(createdAt)
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
