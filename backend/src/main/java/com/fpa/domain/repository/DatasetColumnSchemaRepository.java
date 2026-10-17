package com.fpa.domain.repository;

import com.fpa.domain.entity.DatasetColumnSchema;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DatasetColumnSchemaRepository extends JpaRepository<DatasetColumnSchema, Long> {

    List<DatasetColumnSchema> findByDatasetId(Long datasetId);

    Optional<DatasetColumnSchema> findByDatasetIdAndColumnName(Long datasetId, String columnName);
}
