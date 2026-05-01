package com.example.authserver.controller;

import com.example.authserver.model.LoginRequest;
import com.example.authserver.model.LoginResponse;
import com.example.authserver.util.JwtUtil;
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
 * REST controller that handles authentication requests.
 * <p>
 * Endpoint:
 * POST /auth/login
 * Body: { "username": "alice", "password": "password123" }
 * Returns: { "token": "eyJ...", "tokenType": "Bearer", "expiresIn": 86400000 }
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
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

            // If we reach here, credentials were valid — generate a token
            String token = jwtUtil.generateToken(auth);
            return ResponseEntity.ok(new LoginResponse(token));

        } catch (BadCredentialsException ex) {
            return ResponseEntity
                    .status(401)
                    .body("Invalid username or password");
        } catch (Exception e) {
            return ResponseEntity
                    .status(500)
                    .body(e.getMessage());
        }
    }
}
