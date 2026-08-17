package com.bank.gateway.dto;

public record LoginRequest(String username, String password) {

    public record Response(String accessToken, String tokenType, long expiresInMinutes) {
    }
}
