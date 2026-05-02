package com.example.authserver.controller;

import com.example.authserver.model.*;
import com.example.authserver.service.RefreshTokenService;
import com.example.authserver.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that handles the full authentication lifecycle.
 * <p>
 * Endpoints:
 * <pre>
 *   POST /auth/login
 *     Body:    { "username": "alice", "password": "password123" }
 *     Returns: { "accessToken": "eyJ...", "refreshToken": "uuid", "tokenType": "Bearer", "expiresIn": 20000 }
 *
 *   POST /auth/refresh
 *     Body:    { "refreshToken": "uuid" }
 *     Returns: same shape as login — both tokens are rotated
 *
 *   POST /auth/logout
 *     Body:    { "refreshToken": "uuid" }
 *     Returns: 200 "Logged out successfully"
 * </pre>
 * Access tokens are short-lived JWTs (RS256). Refresh tokens are opaque UUIDs
 * stored server-side with a 7-day TTL.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.expiration}")
    private long accessTokenExpiration;


    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Authenticates the user and returns a signed JWT on success.
     * <p>
     * Spring Security's AuthenticationManager handles the credential check:
     * 1. Loads the user from UserDetailsService
     * 2. Compares the password using BCryptPasswordEncoder
     * 3. Throws BadCredentialsException if they don't match
     *
     * @param request login credentials from the request body
     * @return 200 OK with JWT, or 401 Unauthorized if credentials are wrong
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // This line does all the credential validation work
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );

            String accessToken = jwtUtil.generateToken(auth);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(auth);

            return ResponseEntity.ok(new LoginResponse(accessToken, refreshToken.getToken(),accessTokenExpiration));

        }
        catch (BadCredentialsException ex) {
            return ResponseEntity.status(401).body("Invalid username or password");
        }
        catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }


    /**
     * POST /auth/refresh
     * Validates the refresh token and issues a new access token.
     * The old refresh token is revoked and a new one is issued (rotation).
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        try {
            RefreshToken refreshTokenObj = refreshTokenService.validateRefreshToken(request.refreshToken());

            // Rotate: revoke old refresh token, issue a new one
            // This limits the damage if a refresh token is stolen
            refreshTokenService.revokeRefreshToken(request.refreshToken());
            RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(refreshTokenObj.getUsername(), refreshTokenObj.getRoles());

            String newAccessToken = jwtUtil.generateToken(newRefreshToken);

            return ResponseEntity.ok(new LoginResponse(newAccessToken, newRefreshToken.getToken(),accessTokenExpiration));
        }
        catch (RuntimeException ex) {
            return ResponseEntity.status(401).body(ex.getMessage());
        }
    }

    /**
     * POST /auth/logout
     * Revokes the refresh token — the access token will naturally expire.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest request) {
        refreshTokenService.revokeRefreshToken(request.refreshToken());
        return ResponseEntity.ok("Logged out successfully");
    }
}
