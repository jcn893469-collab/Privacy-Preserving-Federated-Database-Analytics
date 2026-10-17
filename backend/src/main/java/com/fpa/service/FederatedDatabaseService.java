package com.fpa.service;

import com.fpa.api.dto.DatabaseRegistrationRequest;
import com.fpa.api.dto.DatabaseResponse;
import com.fpa.domain.entity.FederatedDatabase;
import com.fpa.domain.entity.User;
import com.fpa.domain.repository.FederatedDatabaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FederatedDatabaseService {

    private final FederatedDatabaseRepository federatedDatabaseRepository;

    public FederatedDatabaseService(FederatedDatabaseRepository federatedDatabaseRepository) {
        this.federatedDatabaseRepository = federatedDatabaseRepository;
    }

    @Transactional
    public DatabaseResponse registerForOwner(DatabaseRegistrationRequest request, User owner) {
        if (owner.getRole() != User.Role.DATA_OWNER && owner.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("Only DATA_OWNER accounts can register an organization database");
        }
        String identifier = request.getDbIdentifier().trim();
        if (federatedDatabaseRepository.findByDbIdentifier(identifier).isPresent()) {
            throw new IllegalArgumentException("A database with this identifier already exists: " + identifier);
        }

        FederatedDatabase database = FederatedDatabase.builder()
            .organizationName(request.getOrganizationName().trim())
            .dbIdentifier(identifier)
            .description(request.getDescription())
            .owner(owner)
            .active(true)
            .registeredAt(LocalDateTime.now())
            .privacyEpsilonBudget(BigDecimal.valueOf(10.0))
            .privacyDeltaBudget(BigDecimal.valueOf(1e-5))
            .privacyEpsilonSpent(BigDecimal.ZERO)
            .build();

        return toResponse(federatedDatabaseRepository.save(database));
    }

    @Transactional(readOnly = true)
    public List<DatabaseResponse> getAllDatabases() {
        return federatedDatabaseRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<DatabaseResponse> getPaginatedDatabases(Pageable pageable) {
        return federatedDatabaseRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<DatabaseResponse> searchAndFilter(String query, Boolean active, Pageable pageable) {
        return federatedDatabaseRepository.searchAndFilter(query, active, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DatabaseResponse getDatabaseById(Long id) {
        return toResponse(require(id));
    }

    @Transactional(readOnly = true)
    public List<DatabaseResponse> getDatabasesOwnedBy(User owner) {
        return federatedDatabaseRepository.findByOwnerId(owner.getId())
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<DatabaseResponse> getDatabasesOwnedByPaginated(User owner, Pageable pageable) {
        return federatedDatabaseRepository.findByOwnerId(owner.getId(), pageable).map(this::toResponse);
    }

    @Transactional
    public DatabaseResponse updateDatabase(Long id, String organizationName, String description, User caller) {
        FederatedDatabase database = require(id);
        if (caller.getRole() != User.Role.ADMIN && !database.getOwner().getId().equals(caller.getId())) {
            throw new SecurityException("You do not have permission to modify this database");
        }
        if (organizationName != null && !organizationName.isBlank()) {
            database.setOrganizationName(organizationName.trim());
        }
        if (description != null) {
            database.setDescription(description.trim());
        }
        return toResponse(federatedDatabaseRepository.save(database));
    }

    @Transactional
    public void deleteDatabase(Long id, User caller) {
        FederatedDatabase database = require(id);
        if (caller.getRole() != User.Role.ADMIN && !database.getOwner().getId().equals(caller.getId())) {
            throw new SecurityException("You do not have permission to delete this database");
        }
        federatedDatabaseRepository.delete(database);
    }

    @Transactional
    public DatabaseResponse setActive(Long id, boolean active) {
        FederatedDatabase database = require(id);
        database.setActive(active);
        return toResponse(federatedDatabaseRepository.save(database));
    }

    @Transactional
    public DatabaseResponse updatePrivacyBudget(Long id, BigDecimal epsilonBudget, BigDecimal deltaBudget) {
        FederatedDatabase database = require(id);
        if (epsilonBudget != null) {
            database.setPrivacyEpsilonBudget(epsilonBudget);
        }
        if (deltaBudget != null) {
            database.setPrivacyDeltaBudget(deltaBudget);
        }
        return toResponse(federatedDatabaseRepository.save(database));
    }

    public FederatedDatabase requireOwned(Long databaseId, User owner) {
        FederatedDatabase database = require(databaseId);
        if (!database.getOwner().getId().equals(owner.getId()) && owner.getRole() != User.Role.ADMIN) {
            throw new SecurityException("Organizations can access only their own resources");
        }
        return database;
    }

    public FederatedDatabase require(Long id) {
        return federatedDatabaseRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Database not found with id: " + id));
    }

    public DatabaseResponse toResponse(FederatedDatabase database) {
        return new DatabaseResponse(
            database.getId(),
            database.getOrganizationName(),
            database.getDbIdentifier(),
            database.getDescription(),
            database.getOwner().getId(),
            database.getOwner().getEmail(),
            database.isActive(),
            database.getRegisteredAt(),
            database.getPrivacyEpsilonBudget(),
            database.getPrivacyEpsilonSpent(),
            database.getPrivacyDeltaBudget()
        );
    }
}
