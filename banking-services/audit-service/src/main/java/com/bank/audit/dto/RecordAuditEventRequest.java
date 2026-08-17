package com.bank.audit.dto;

import com.bank.audit.domain.AuditResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record RecordAuditEventRequest(
        @NotBlank String userId,
        @NotBlank String action,
        @NotBlank String resource,
        @NotNull AuditResult result,
        String ipAddress,
        Map<String, Object> details
) {
}
