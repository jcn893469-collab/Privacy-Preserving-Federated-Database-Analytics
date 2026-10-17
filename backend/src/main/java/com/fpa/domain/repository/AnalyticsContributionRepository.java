package com.fpa.domain.repository;

import com.fpa.domain.entity.AnalyticsContribution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface AnalyticsContributionRepository extends JpaRepository<AnalyticsContribution, Long> {

    List<AnalyticsContribution> findByDatabaseId(Long databaseId);

    Page<AnalyticsContribution> findByDatabaseId(Long databaseId, Pageable pageable);

    @Query("SELECT ac FROM AnalyticsContribution ac " +
           "JOIN FETCH ac.database db " +
           "WHERE (:databaseId IS NULL OR db.id = :databaseId) " +
           "AND (:startDate IS NULL OR ac.submittedAt >= :startDate) " +
           "AND (:endDate IS NULL OR ac.submittedAt <= :endDate) " +
           "ORDER BY ac.submittedAt DESC")
    Page<AnalyticsContribution> searchAndFilter(
            @Param("databaseId") Long databaseId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT ac FROM AnalyticsContribution ac " +
           "JOIN FETCH ac.database db " +
           "JOIN FETCH db.owner u " +
           "WHERE (:query IS NULL OR " +
           "       LOWER(db.organizationName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "       LOWER(db.dbIdentifier) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "       LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY ac.submittedAt DESC")
    Page<AnalyticsContribution> searchGlobalContributions(@Param("query") String query, Pageable pageable);

    @Query("SELECT COUNT(ac) FROM AnalyticsContribution ac WHERE ac.database.id = :databaseId")
    long countByDatabaseId(@Param("databaseId") Long databaseId);

    @Query("SELECT COALESCE(SUM(ac.recordCount), 0) FROM AnalyticsContribution ac WHERE ac.database.id = :databaseId")
    long sumRecordCountByDatabaseId(@Param("databaseId") Long databaseId);

    @Query("SELECT COALESCE(SUM(ac.recordCount), 0) FROM AnalyticsContribution ac")
    long sumGlobalRecordCount();

    @Query("SELECT COALESCE(SUM(ac.noisySum), 0.0) FROM AnalyticsContribution ac")
    BigDecimal sumGlobalNoisySum();

    @Query("SELECT COALESCE(AVG(ac.recordCount), 0.0) FROM AnalyticsContribution ac")
    Double avgGlobalRecordCount();

    @Query("SELECT COALESCE(MIN(ac.recordCount), 0) FROM AnalyticsContribution ac")
    Integer minGlobalRecordCount();

    @Query("SELECT COALESCE(MAX(ac.recordCount), 0) FROM AnalyticsContribution ac")
    Integer maxGlobalRecordCount();

    @Query("SELECT COALESCE(AVG(ac.anomalyRate), 0.0) FROM AnalyticsContribution ac")
    Double avgGlobalAnomalyRate();
}