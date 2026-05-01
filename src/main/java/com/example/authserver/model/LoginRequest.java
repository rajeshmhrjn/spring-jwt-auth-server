package com.example.authserver.model;

/**
 * Request body for POST /auth/login
 */
public record LoginRequest(String username, String password) {}
