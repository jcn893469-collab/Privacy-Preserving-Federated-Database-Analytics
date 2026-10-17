package com.fpa.domain.repository;

import com.fpa.domain.entity.FederatedDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FederatedDatabaseRepository extends JpaRepository<FederatedDatabase, Long> {

    Optional<FederatedDatabase> findByDbIdentifier(String dbIdentifier);

    List<FederatedDatabase> findByOwnerId(Long ownerId);

    Page<FederatedDatabase> findByOwnerId(Long ownerId, Pageable pageable);

    Optional<FederatedDatabase> findFirstByOwnerIdOrderByRegisteredAtAsc(Long ownerId);

    long countByActiveTrue();

    @Query("SELECT DISTINCT d.organizationName FROM FederatedDatabase d")
    List<String> findDistinctOrganizationNames();

    @Query("SELECT db FROM FederatedDatabase db " +
           "JOIN FETCH db.owner u " +
           "WHERE (:query IS NULL OR " +
           "       LOWER(db.organizationName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "       LOWER(db.dbIdentifier) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "       LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:active IS NULL OR db.active = :active) " +
           "ORDER BY db.registeredAt DESC")
    Page<FederatedDatabase> searchAndFilter(
            @Param("query") String query,
            @Param("active") Boolean active,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(db.privacyEpsilonSpent), 0) FROM FederatedDatabase db")
    BigDecimal sumTotalPrivacyEpsilonSpent();

    @Query("SELECT COUNT(db) FROM FederatedDatabase db WHERE db.owner.id = :ownerId")
    long countByOwnerId(@Param("ownerId") Long ownerId);
}
