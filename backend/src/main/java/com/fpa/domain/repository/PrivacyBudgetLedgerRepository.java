package com.fpa.domain.repository;

import com.fpa.domain.entity.PrivacyBudgetLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PrivacyBudgetLedgerRepository extends JpaRepository<PrivacyBudgetLedger, Long> {

    List<PrivacyBudgetLedger> findByDatabaseIdOrderByEvaluatedAtDesc(Long databaseId);

    Page<PrivacyBudgetLedger> findByDatabaseId(Long databaseId, Pageable pageable);

    @Query("SELECT pbl FROM PrivacyBudgetLedger pbl " +
           "JOIN FETCH pbl.database db " +
           "JOIN FETCH pbl.contribution ac " +
           "WHERE (:databaseId IS NULL OR db.id = :databaseId) " +
           "ORDER BY pbl.evaluatedAt DESC")
    Page<PrivacyBudgetLedger> findAllWithDetails(@Param("databaseId") Long databaseId, Pageable pageable);
}
