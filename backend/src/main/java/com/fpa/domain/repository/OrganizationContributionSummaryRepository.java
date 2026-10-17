package com.fpa.domain.repository;

import com.fpa.domain.entity.OrganizationContributionSummaryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrganizationContributionSummaryRepository extends JpaRepository<OrganizationContributionSummaryView, Long> {

    @Query("SELECT v FROM OrganizationContributionSummaryView v " +
           "WHERE (:query IS NULL OR " +
           "       LOWER(v.organizationName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "       LOWER(v.dbIdentifier) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "       LOWER(v.ownerEmail) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY v.organizationName ASC")
    Page<OrganizationContributionSummaryView> searchView(
            @Param("query") String query,
            Pageable pageable);

    List<OrganizationContributionSummaryView> findAllByOrderByOrganizationNameAsc();
}
