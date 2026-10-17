package com.fpa.domain.repository;

import com.fpa.domain.entity.FederatedResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FederatedResultRepository extends JpaRepository<FederatedResult, Long> {

    Optional<FederatedResult> findByRunIdentifier(String runIdentifier);

    @Query("SELECT fr FROM FederatedResult fr " +
           "LEFT JOIN FETCH fr.executedBy u " +
           "WHERE (:query IS NULL OR " +
           "       LOWER(fr.runIdentifier) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "       LOWER(fr.targetMetric) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY fr.executedAt DESC")
    Page<FederatedResult> searchRuns(@Param("query") String query, Pageable pageable);

    Optional<FederatedResult> findFirstByOrderByExecutedAtDesc();
}
