package com.fpa.api.controller;

import com.fpa.api.dto.AdminSystemStatusResponse;
import com.fpa.api.dto.AuditLogResponse;
import com.fpa.domain.entity.AuditLog;
import com.fpa.domain.entity.OrganizationContributionSummaryView;
import com.fpa.domain.entity.User;
import com.fpa.security.CurrentUserService;
import com.fpa.service.AdminDashboardService;
import com.fpa.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminDashboardService adminDashboardService;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;

    public AdminController(AdminDashboardService adminDashboardService,
                           AuditLogService auditLogService,
                           CurrentUserService currentUserService) {
        this.adminDashboardService = adminDashboardService;
        this.auditLogService = auditLogService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/status")
    public ResponseEntity<AdminSystemStatusResponse> getSystemStatus() {
        currentUserService.requireRole(User.Role.ADMIN);
        return ResponseEntity.ok(adminDashboardService.status());
    }

    @GetMapping("/views/organization-summary")
    public ResponseEntity<?> getOrganizationSummaries(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        currentUserService.requireRole(User.Role.ADMIN);
        if (query != null && !query.isBlank()) {
            Pageable pageable = PageRequest.of(page, size);
            return ResponseEntity.ok(adminDashboardService.searchOrganizationSummaries(query, pageable));
        }
        return ResponseEntity.ok(adminDashboardService.getOrganizationSummaries());
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        currentUserService.requireRole(User.Role.ADMIN);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(auditLogService.searchAndFilter(action, entityType, startDate, endDate, query, pageable)
            .map(AuditLogResponse::from));
    }
}
