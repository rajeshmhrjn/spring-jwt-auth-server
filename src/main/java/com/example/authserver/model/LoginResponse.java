package com.example.authserver.model;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn)
{
    public LoginResponse(String accessToken, String refreshToken, long expiresIn) {
        this(accessToken, refreshToken, "Bearer", expiresIn);
    }
}