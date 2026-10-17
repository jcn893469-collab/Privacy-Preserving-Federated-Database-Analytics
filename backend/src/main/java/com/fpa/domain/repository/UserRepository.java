package com.fpa.domain.repository;

import com.fpa.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(User.Role role);

    List<User> findByRole(User.Role role);

    @Query("SELECT u FROM User u " +
           "WHERE (:query IS NULL OR " +
           "       LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "       LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:role IS NULL OR u.role = :role) " +
           "AND (:active IS NULL OR u.active = :active) " +
           "ORDER BY u.createdAt DESC")
    Page<User> searchAndFilter(
            @Param("query") String query,
            @Param("role") User.Role role,
            @Param("active") Boolean active,
            Pageable pageable);
}
