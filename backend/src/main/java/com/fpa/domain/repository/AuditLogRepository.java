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
           "WHERE (:action IS NULL OR al.action = :action) " +
           "AND (:entityType IS NULL OR al.entityType = :entityType) " +
           "AND (:startDate IS NULL OR al.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR al.createdAt <= :endDate) " +
           "AND (:query IS NULL OR " +
           "     LOWER(al.userEmail) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(al.details) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY al.createdAt DESC")
    Page<AuditLog> searchAndFilter(
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("query") String query,
            Pageable pageable);
}
