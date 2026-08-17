package com.bank.audit.dto;

import com.bank.audit.domain.AuditEvent;
import com.bank.audit.domain.AuditResult;

import java.time.Instant;

public record AuditEventResponse(
        String auditId,
        String userId,
        String action,
        String resource,
        AuditResult result,
        String ipAddress,
        String details,
        Instant timestamp
) {
    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(event.getAuditId(), event.getUserId(), event.getAction(),
                event.getResource(), event.getResult(), event.getIpAddress(), event.getDetails(),
                event.getTimestamp());
    }
}
