package com.bank.common.api;

import java.time.Instant;

/**
 * Standardized error envelope returned by every service in the platform.
 * Mirrors the "Exception Handling" contract in the architecture plan (section 38).
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String correlationId
) {
    public static ErrorResponse of(int status, String code, String message, String correlationId) {
        return new ErrorResponse(Instant.now(), status, code, message, correlationId);
    }
}
