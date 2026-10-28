package com.fpa.api.dto;

import com.fpa.domain.entity.AuditLog;

import java.time.LocalDateTime;

public record AuditLogResponse(
    Long id,
    String action,
    String entityType,
    Long entityId,
    String userEmail,
    String ipAddress,
    String details,
    String status,
    LocalDateTime createdAt
) {
    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
            auditLog.getId(),
            auditLog.getAction(),
            auditLog.getEntityType(),
            auditLog.getEntityId(),
            auditLog.getUserEmail(),
            auditLog.getIpAddress(),
            auditLog.getDetails(),
            auditLog.getStatus(),
            auditLog.getCreatedAt()
        );
    }
}
