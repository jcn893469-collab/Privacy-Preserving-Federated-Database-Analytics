package com.fpa.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dataset_metadata")
public class DatasetMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "database_id", nullable = false)
    private FederatedDatabase database;

    @OneToMany(mappedBy = "dataset", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DatasetColumnSchema> columnSchemas = new ArrayList<>();

    @Column(name = "dataset_name", nullable = false, length = 150)
    private String datasetName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "schema_version", nullable = false, length = 30)
    private String schemaVersion = "1.0";

    @Column(name = "total_records", nullable = false)
    private Integer totalRecords = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public DatasetMetadata() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public FederatedDatabase getDatabase() { return database; }
    public void setDatabase(FederatedDatabase database) { this.database = database; }
    public List<DatasetColumnSchema> getColumnSchemas() { return columnSchemas; }
    public String getDatasetName() { return datasetName; }
    public void setDatasetName(String datasetName) { this.datasetName = datasetName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }
    public Integer getTotalRecords() { return totalRecords; }
    public void setTotalRecords(Integer totalRecords) { this.totalRecords = totalRecords; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final DatasetMetadata target = new DatasetMetadata();

        public Builder database(FederatedDatabase database) { target.database = database; return this; }
        public Builder datasetName(String datasetName) { target.datasetName = datasetName; return this; }
        public Builder description(String description) { target.description = description; return this; }
        public Builder schemaVersion(String schemaVersion) { target.schemaVersion = schemaVersion; return this; }
        public Builder totalRecords(Integer totalRecords) { target.totalRecords = totalRecords; return this; }
        public Builder createdAt(LocalDateTime createdAt) { target.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { target.updatedAt = updatedAt; return this; }
        public DatasetMetadata build() { return target; }
    }
}
