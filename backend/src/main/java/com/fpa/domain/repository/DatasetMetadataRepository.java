package com.fpa.domain.repository;

import com.fpa.domain.entity.DatasetMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DatasetMetadataRepository extends JpaRepository<DatasetMetadata, Long> {

    List<DatasetMetadata> findByDatabaseId(Long databaseId);

    Page<DatasetMetadata> findByDatabaseId(Long databaseId, Pageable pageable);

    Optional<DatasetMetadata> findByDatabaseIdAndDatasetName(Long databaseId, String datasetName);

    @Query("SELECT dm FROM DatasetMetadata dm " +
           "JOIN FETCH dm.database db " +
           "WHERE (:databaseId IS NULL OR db.id = :databaseId) " +
           "AND (TRIM(COALESCE(:query, '')) = '' OR " +
           "LOWER(dm.datasetName) LIKE LOWER(CONCAT('%', TRIM(COALESCE(:query, '')), '%'))) " +
           "ORDER BY dm.createdAt DESC")
    Page<DatasetMetadata> searchAndFilter(
            @Param("databaseId") Long databaseId,
            @Param("query") String query,
            Pageable pageable);
}
