package com.fpa.api.controller;

import com.fpa.api.dto.DatabaseRegistrationRequest;
import com.fpa.api.dto.DatabaseResponse;
import com.fpa.api.dto.PrivacyStatusResponse;
import com.fpa.domain.entity.FederatedDatabase;
import com.fpa.domain.entity.User;
import com.fpa.security.CurrentUserService;
import com.fpa.service.AnalyticsContributionService;
import com.fpa.service.FederatedDatabaseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/databases")
public class FederatedDatabaseController {

    private final FederatedDatabaseService federatedDatabaseService;
    private final AnalyticsContributionService analyticsContributionService;
    private final CurrentUserService currentUserService;

    public FederatedDatabaseController(FederatedDatabaseService federatedDatabaseService,
                                       AnalyticsContributionService analyticsContributionService,
                                       CurrentUserService currentUserService) {
        this.federatedDatabaseService = federatedDatabaseService;
        this.analyticsContributionService = analyticsContributionService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<?> registerDatabase(@Valid @RequestBody DatabaseRegistrationRequest request) {
        User owner = currentUserService.requireAnyRole(User.Role.DATA_OWNER, User.Role.ADMIN);
        DatabaseResponse response = federatedDatabaseService.registerForOwner(request, owner);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<?> getAllDatabases(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("registeredAt").descending());
        User caller = currentUserService.requireAnyRole(User.Role.ADMIN, User.Role.DATA_OWNER);
        if (caller.getRole() == User.Role.DATA_OWNER) {
            if ((query != null && !query.isBlank()) || active != null) {
                return ResponseEntity.ok(federatedDatabaseService.searchOwned(caller, query, active, pageable));
            }
            return ResponseEntity.ok(federatedDatabaseService.getDatabasesOwnedByPaginated(caller, pageable));
        }
        if (query != null || active != null) {
            return ResponseEntity.ok(federatedDatabaseService.searchAndFilter(query, active, pageable));
        }
        return ResponseEntity.ok(federatedDatabaseService.getPaginatedDatabases(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDatabaseById(@PathVariable Long id) {
        User caller = currentUserService.requireAnyRole(User.Role.ADMIN, User.Role.DATA_OWNER);
        if (caller.getRole() == User.Role.DATA_OWNER) {
            federatedDatabaseService.requireOwned(id, caller);
        }
        return ResponseEntity.ok(federatedDatabaseService.getDatabaseById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDatabase(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        User caller = currentUserService.requireAnyRole(User.Role.ADMIN, User.Role.DATA_OWNER);
        String orgName = body.get("organizationName");
        String description = body.get("description");
        DatabaseResponse response = federatedDatabaseService.updateDatabase(id, orgName, description, caller);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDatabase(@PathVariable Long id) {
        User caller = currentUserService.requireAnyRole(User.Role.ADMIN, User.Role.DATA_OWNER);
        federatedDatabaseService.deleteDatabase(id, caller);
        return ResponseEntity.ok(Map.of("message", "Database deleted successfully", "id", id));
    }

    @GetMapping("/mine")
    public ResponseEntity<?> getMyDatabases() {
        User owner = currentUserService.requireRole(User.Role.DATA_OWNER);
        List<DatabaseResponse> databases = federatedDatabaseService.getDatabasesOwnedBy(owner);
        return ResponseEntity.ok(databases);
    }

    @GetMapping("/mine/privacy")
    public ResponseEntity<?> getMyPrivacyStatus() {
        User owner = currentUserService.requireRole(User.Role.DATA_OWNER);
        List<FederatedDatabase> owned = federatedDatabaseService.getDatabasesOwnedBy(owner).stream()
                .map(d -> federatedDatabaseService.require(d.getId()))
                .toList();

        if (owned.isEmpty()) {
            return ResponseEntity.ok(Map.of("error", "No database registered for this account"));
        }
        PrivacyStatusResponse status = analyticsContributionService.privacyStatusFor(owned.get(0));
        return ResponseEntity.ok(status);
    }
}