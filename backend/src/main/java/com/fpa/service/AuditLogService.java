package com.fpa.service;

import com.fpa.domain.entity.AuditLog;
import com.fpa.domain.entity.User;
import com.fpa.domain.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AuditLog logEvent(User user, String action, String entityType, Long entityId,
                             String details, String ipAddress, String status) {
        AuditLog entry = AuditLog.builder()
                .user(user)
                .userEmail(user != null ? user.getEmail() : "ANONYMOUS")
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .ipAddress(ipAddress != null ? ipAddress : "127.0.0.1")
                .status(status != null ? status : "SUCCESS")
                .createdAt(LocalDateTime.now())
                .build();
        return auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> searchAndFilter(String action, String entityType,
                                          LocalDateTime startDate, LocalDateTime endDate,
                                          String query, Pageable pageable) {
        String normalizedQuery = query == null || query.isBlank() ? "" : query.trim();
        return auditLogRepository.searchAndFilter(
            action != null && !action.isBlank(), action == null ? "" : action.trim(),
            entityType != null && !entityType.isBlank(), entityType == null ? "" : entityType.trim(),
            startDate != null, startDate, endDate != null, endDate, normalizedQuery, pageable
        );
    }
}
