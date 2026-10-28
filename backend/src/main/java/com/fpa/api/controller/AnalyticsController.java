package com.fpa.api.controller;

import com.fpa.api.dto.ContributionRequest;
import com.fpa.api.dto.ContributionResponse;
import com.fpa.api.dto.FederatedResultResponse;
import com.fpa.api.dto.FederatedRunResponse;
import com.fpa.api.dto.PrivacyStatusResponse;
import com.fpa.domain.entity.FederatedDatabase;
import com.fpa.domain.entity.FederatedResult;
import com.fpa.domain.entity.User;
import com.fpa.security.CurrentUserService;
import com.fpa.service.AnalyticsContributionService;
import com.fpa.service.FederatedAggregationService;
import com.fpa.service.FederatedDatabaseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsContributionService analyticsContributionService;
    private final FederatedAggregationService federatedAggregationService;
    private final FederatedDatabaseService federatedDatabaseService;
    private final CurrentUserService currentUserService;

    public AnalyticsController(AnalyticsContributionService analyticsContributionService,
                               FederatedAggregationService federatedAggregationService,
                               FederatedDatabaseService federatedDatabaseService,
                               CurrentUserService currentUserService) {
        this.analyticsContributionService = analyticsContributionService;
        this.federatedAggregationService = federatedAggregationService;
        this.federatedDatabaseService = federatedDatabaseService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/contributions")
    public ResponseEntity<?> submitContribution(@Valid @RequestBody ContributionRequest request) {
        ContributionResponse response = analyticsContributionService.submitContribution(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/contributions")
    public ResponseEntity<?> getContributions(
            @RequestParam(required = false) Long databaseId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());
        User caller = currentUserService.requireAnyRole(User.Role.ADMIN, User.Role.DATA_OWNER);
        if (caller.getRole() == User.Role.DATA_OWNER) {
            if (databaseId != null) {
                federatedDatabaseService.requireOwned(databaseId, caller);
            }
            return ResponseEntity.ok(analyticsContributionService.searchOwnedContributions(
                caller, databaseId, startDate, endDate, query, pageable
            ));
        }
        if (query != null && !query.isBlank()) {
            return ResponseEntity.ok(analyticsContributionService.searchGlobalContributions(query, pageable));
        }
        if (databaseId != null || startDate != null || endDate != null) {
            return ResponseEntity.ok(analyticsContributionService.searchAndFilterContributions(databaseId, startDate, endDate, pageable));
        }
        return ResponseEntity.ok(analyticsContributionService.getPaginatedContributions(pageable));
    }

    @GetMapping("/contributions/{id}")
    public ResponseEntity<?> getContributionById(@PathVariable Long id) {
        User caller = currentUserService.requireAnyRole(User.Role.ADMIN, User.Role.DATA_OWNER);
        return ResponseEntity.ok(analyticsContributionService.getContributionById(id, caller));
    }

    @DeleteMapping("/contributions/{id}")
    public ResponseEntity<?> deleteContribution(@PathVariable Long id) {
        analyticsContributionService.deleteContribution(id);
        return ResponseEntity.ok(Map.of("message", "Contribution deleted successfully", "id", id));
    }

    @GetMapping("/results/{databaseId}")
    public ResponseEntity<?> getFederatedResult(@PathVariable Long databaseId) {
        User caller = currentUserService.requireAnyRole(User.Role.ADMIN, User.Role.DATA_OWNER, User.Role.RESEARCHER);
        if (caller.getRole() == User.Role.DATA_OWNER) {
            federatedDatabaseService.requireOwned(databaseId, caller);
        }
        FederatedResultResponse response = analyticsContributionService.computeFederatedResult(databaseId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/results/global")
    public ResponseEntity<?> getGlobalFederatedResult() {
        currentUserService.requireAnyRole(User.Role.ADMIN, User.Role.RESEARCHER);
        FederatedResultResponse response = analyticsContributionService.computeGlobalFederatedResult();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/results/runs")
    public ResponseEntity<?> executeFederatedRun(@RequestParam(defaultValue = "overall") String metric) {
        currentUserService.requireAnyRole(User.Role.ADMIN, User.Role.RESEARCHER);
        FederatedResult run = federatedAggregationService.executeAndPersistFederatedRun(metric);
        return ResponseEntity.status(HttpStatus.CREATED).body(FederatedRunResponse.from(run));
    }

    @GetMapping("/results/runs")
    public ResponseEntity<?> getHistoricalRuns(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        currentUserService.requireAnyRole(User.Role.ADMIN, User.Role.RESEARCHER);
        Pageable pageable = PageRequest.of(page, size, Sort.by("executedAt").descending());
        return ResponseEntity.ok(federatedAggregationService.getHistoricalRuns(query, pageable)
            .map(FederatedRunResponse::from));
    }

    @GetMapping("/results/runs/{id}")
    public ResponseEntity<?> getRunById(@PathVariable Long id) {
        currentUserService.requireAnyRole(User.Role.ADMIN, User.Role.RESEARCHER);
        return ResponseEntity.ok(FederatedRunResponse.from(federatedAggregationService.getRunById(id)));
    }

    @GetMapping("/privacy-status/{databaseId}")
    public ResponseEntity<?> getPrivacyStatus(@PathVariable Long databaseId) {
        User caller = currentUserService.requireAnyRole(User.Role.ADMIN, User.Role.DATA_OWNER);
        if (caller.getRole() == User.Role.DATA_OWNER) {
            federatedDatabaseService.requireOwned(databaseId, caller);
        }
        FederatedDatabase db = federatedDatabaseService.require(databaseId);
        PrivacyStatusResponse status = analyticsContributionService.privacyStatusFor(db);
        return ResponseEntity.ok(status);
    }
}