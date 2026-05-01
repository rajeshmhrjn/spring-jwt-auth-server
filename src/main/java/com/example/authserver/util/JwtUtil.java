package com.example.authserver.util;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.util.Date;

/**
 * Utility class responsible for generating and signing JWT tokens.
 * <p>
 * The token includes:
 * - subject: the username
 * - issuedAt: current timestamp
 * - expiration: current time + configured expiry
 * - signature: HMAC-SHA256 using the shared secret
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
     * Generates a signed JWT token for the given username.
     *
     * @param authentication@return a compact, URL-safe JWT string (e.g. "eyJ...")
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
}