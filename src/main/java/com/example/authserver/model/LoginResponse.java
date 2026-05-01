package com.example.authserver.model;

/**
 * Response body for POST /auth/login
 */
public record LoginResponse(String token, String tokenType, long expiresIn) {

    public LoginResponse(String token) {
        this(token, "Bearer", 86400000L);
    }
}
