package com.example.authserver.model;

import java.time.Instant;

/**
 * Represents a refresh token stored in the database.
 * Using a simple in-memory map for demo — replace with JPA entity in production.
 */
public class RefreshToken {

    private String token;        // the random token string
    private String username;
    private String roles;
    private Instant expiresAt;   // when it expires

    public RefreshToken(String token, String username, String roles, Instant expiresAt) {
        this.token = token;
        this.username = username;
        this.roles = roles;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}