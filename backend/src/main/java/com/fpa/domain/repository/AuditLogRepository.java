package com.fpa.domain.repository;

import com.fpa.domain.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT al FROM AuditLog al " +
           "WHERE (:filterAction = false OR al.action = :action) " +
           "AND (:filterEntityType = false OR al.entityType = :entityType) " +
           "AND (:filterStartDate = false OR al.createdAt >= :startDate) " +
           "AND (:filterEndDate = false OR al.createdAt <= :endDate) " +
           "AND (LOWER(al.userEmail) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(al.details) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY al.createdAt DESC")
    Page<AuditLog> searchAndFilter(
            @Param("filterAction") boolean filterAction,
            @Param("action") String action,
            @Param("filterEntityType") boolean filterEntityType,
            @Param("entityType") String entityType,
            @Param("filterStartDate") boolean filterStartDate,
            @Param("startDate") LocalDateTime startDate,
            @Param("filterEndDate") boolean filterEndDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("query") String query,
            Pageable pageable);
}
