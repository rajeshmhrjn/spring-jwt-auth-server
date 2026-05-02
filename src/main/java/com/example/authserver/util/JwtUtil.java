package com.example.authserver.util;

import com.example.authserver.model.RefreshToken;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.util.Date;

/**
 * Generates and signs short-lived JWT access tokens using RS256 (RSA + SHA-256).
 * <p>
 * Each token contains:
 * - {@code sub}   — the username
 * - {@code roles} — comma-separated list of granted authorities
 * - {@code iat}   — issued-at timestamp
 * - {@code exp}   — expiry (iat + {@code jwt.expiration} ms)
 * - {@code kid}   — key ID in the JWT header, matched against JWKS for verification
 * <p>
 * Resource servers validate signatures offline by fetching the RSA public key
 * from {@code /.well-known/jwks.json}.
 */
@Component
public class JwtUtil {
    private final KeyPair keyPair;
    private static final String KEY_ID = "auth-server-key-v1";

    @Value("${jwt.expiration}")
    private long expiration;

    public JwtUtil(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    /**
     * Generates a signed JWT access token from a Spring Security {@link Authentication}.
     * Called after a successful login to produce the initial access token.
     *
     * @param authentication the authenticated principal returned by {@link org.springframework.security.authentication.AuthenticationManager}
     * @return a compact, URL-safe JWT string (e.g. {@code eyJ...})
     */
    public String generateToken(Authentication authentication) {
        return Jwts.builder()
                .header()
                .keyId(KEY_ID)        // <-- stamps "kid" into the JWT header
                .and()
                .subject(authentication.getName())
                .claim("roles", authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(keyPair.getPrivate())
                .compact();
    }

    /**
     * Generates a signed JWT access token from a {@link RefreshToken}.
     * Called during token refresh — the refresh token carries the username and roles
     * so a new access token can be issued without re-authenticating the user.
     *
     * @param refreshToken a validated, non-expired refresh token
     * @return a compact, URL-safe JWT string (e.g. {@code eyJ...})
     */
    public String generateToken(RefreshToken refreshToken) {
        return Jwts.builder()
                .header()
                .keyId(KEY_ID)        // <-- stamps "kid" into the JWT header
                .and()
                .subject(refreshToken.getUsername())
                .claim("roles", refreshToken.getRoles().split(","))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(keyPair.getPrivate())
                .compact();
    }
}