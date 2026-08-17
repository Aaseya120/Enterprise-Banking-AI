package com.bank.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Errors originating in the AI plane: model provider failures, vector search
 * failures, guardrail rejections, etc. Kept distinct from BusinessException
 * so callers can decide whether to fall back to a degraded (non-AI) path.
 */
public class AiPlatformException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public AiPlatformException(String code, String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }

    public static AiPlatformException providerError(String message, Throwable cause) {
        return new AiPlatformException("AI_PROVIDER_ERROR", message, HttpStatus.BAD_GATEWAY, cause);
    }

    public static AiPlatformException vectorSearchError(String message, Throwable cause) {
        return new AiPlatformException("VECTOR_SEARCH_ERROR", message, HttpStatus.BAD_GATEWAY, cause);
    }

    public static AiPlatformException guardrailRejected(String message) {
        return new AiPlatformException("GUARDRAIL_REJECTED", message, HttpStatus.UNPROCESSABLE_ENTITY, null);
    }

    public static AiPlatformException toolInvocationError(String message, Throwable cause) {
        return new AiPlatformException("TOOL_INVOCATION_ERROR", message, HttpStatus.BAD_GATEWAY, cause);
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
