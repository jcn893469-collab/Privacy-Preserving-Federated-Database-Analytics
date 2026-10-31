package com.fpa.domain.repository;

import com.fpa.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.tokenVersion = u.tokenVersion + 1 WHERE u.id = :id")
    int incrementTokenVersion(@Param("id") Long id);

    long countByRole(User.Role role);

    List<User> findByRole(User.Role role);

    @Query("SELECT u FROM User u " +
           "WHERE (LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "       LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:filterRole = false OR u.role = :role) " +
           "AND (:filterActive = false OR u.active = :active) " +
           "ORDER BY u.createdAt DESC")
    Page<User> searchAndFilter(
            @Param("query") String query,
            @Param("filterRole") boolean filterRole,
            @Param("role") User.Role role,
            @Param("filterActive") boolean filterActive,
            @Param("active") Boolean active,
            Pageable pageable);
}
