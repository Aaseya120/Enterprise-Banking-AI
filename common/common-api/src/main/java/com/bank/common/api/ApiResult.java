package com.bank.common.api;

/**
 * Lightweight success envelope, used where callers need a correlation id
 * alongside the payload (e.g. AI orchestrator responses).
 */
public record ApiResult<T>(T data, String correlationId) {
    public static <T> ApiResult<T> of(T data, String correlationId) {
        return new ApiResult<>(data, correlationId);
    }
}
