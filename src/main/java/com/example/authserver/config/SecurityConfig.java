package com.example.authserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configures the security filter chain for a stateless JWT-based API.
     * <p>
     * Public endpoints (no token required):
     * - {@code POST /auth/login}    — issues access + refresh tokens
     * - {@code POST /auth/refresh}  — rotates refresh token and issues new access token
     * - {@code POST /auth/logout}   — revokes refresh token
     * - {@code GET  /.well-known/jwks.json} — exposes public key for offline verification
     * <p>
     * All other endpoints require a valid Bearer token in the Authorization header.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — not needed for stateless REST APIs
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless: no HttpSession will be created or used
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login", "/auth/refresh", "/auth/logout","/.well-known/jwks.json").permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    /**
     * Exposes the AuthenticationManager bean so the AuthController can use it
     * to validate credentials via Spring Security's built-in logic.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * In-memory user store for demonstration.
     * <p>
     * Replace this with a JPA-backed UserDetailsService in production:
     * - Load users from a database
     * - Store hashed passwords (BCrypt)
     * - Add roles / authorities as needed
     * <p>
     * Demo users:
     * alice / password123  (role: USER)
     * admin / admin123     (roles: USER, ADMIN)
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails alice = User.builder()
                .username("alice")
                .password(encoder.encode("password123"))
                .roles("USER")
                .build();

        UserDetails admin = User.builder()
                .username("admin")
                .password(encoder.encode("admin123"))
                .roles("USER", "ADMIN")
                .build();

        return new InMemoryUserDetailsManager(alice, admin);
    }

    /**
     * BCrypt password encoder — the industry standard for hashing passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
