package com.bank.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for domain/business-rule violations. Services should throw
 * subclasses of this (or this directly) rather than raw RuntimeExceptions
 * so GlobalExceptionHandler can map them to a standardized ErrorResponse.
 */
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public BusinessException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static BusinessException notFound(String message) {
        return new BusinessException("RESOURCE_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }

    public static BusinessException validation(String message) {
        return new BusinessException("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST);
    }

    public static BusinessException ruleViolation(String message) {
        return new BusinessException("BUSINESS_RULE_VIOLATION", message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException("FORBIDDEN", message, HttpStatus.FORBIDDEN);
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
