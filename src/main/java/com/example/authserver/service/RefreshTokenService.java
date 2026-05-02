package com.example.authserver.service;

import com.example.authserver.model.RefreshToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages refresh tokens.
 *
 * Uses an in-memory ConcurrentHashMap for demo purposes.
 * In production: replace with a JPA repository backed by a database table.
 *
 * DB table would look like:
 *   CREATE TABLE refresh_tokens (
 *     token       VARCHAR(255) PRIMARY KEY,
 *     username    VARCHAR(255) NOT NULL,
 *     expires_at  TIMESTAMP   NOT NULL
 *   );
 */
@Service
public class RefreshTokenService {

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    // In-memory store — replace with RefreshTokenRepository in production
    private final Map<String, RefreshToken> store = new ConcurrentHashMap<>();

    /**
     * Creates a new refresh token for the given username and stores it.
     */
    public RefreshToken createRefreshToken(String username, String roles) {
        String tokenValue = UUID.randomUUID().toString(); // random, opaque, unguessable

        RefreshToken refreshToken = new RefreshToken(
                tokenValue,
                username,
                roles,
                Instant.now().plusMillis(refreshExpiration)
        );

        store.put(tokenValue, refreshToken);
        return refreshToken;
    }

    /**
     * Looks up and validates the refresh token.
     * Throws if not found or expired.
     */
    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = store.get(token);

        if (refreshToken == null) {
            throw new RuntimeException("Refresh token not found");
        }

        if (refreshToken.isExpired()) {
            store.remove(token); // clean up expired token
            throw new RuntimeException("Refresh token has expired. Please log in again.");
        }

        return refreshToken;
    }

    /**
     * Deletes the refresh token — called on logout.
     */
    public void revokeRefreshToken(String token) {
        store.remove(token);
    }

    /**
     * Creates a new refresh token from a Spring Security {@link Authentication}.
     * Convenience overload used at login time so the caller doesn't need to
     * extract username and roles manually.
     */
    public RefreshToken createRefreshToken(Authentication auth) {
        String tokenValue = UUID.randomUUID().toString(); // random, opaque, unguessable

        RefreshToken refreshToken = new RefreshToken(
                tokenValue,
                auth.getName(),
                auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(",")),
                Instant.now().plusMillis(refreshExpiration)
        );

        store.put(tokenValue, refreshToken);
        return refreshToken;
    }
}