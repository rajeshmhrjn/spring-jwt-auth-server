package com.example.authserver.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Map;

@RestController
public class JwksController {

    private final KeyPair keyPair;
    private static final String KEY_ID = "auth-server-key-v1";

    public JwksController(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    /**
     * Standard JWKS endpoint — resource-server fetches public key(s) from here.
     * GET http://localhost:8080/.well-known/jwks.json
     */
    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        RSAPublicKey pub = (RSAPublicKey) keyPair.getPublic();

        Map<String, Object> key = Map.of(
                "kty", "RSA",
                "use", "sig",
                "alg", "RS256",
                "kid", KEY_ID,
                "n", Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(pub.getModulus().toByteArray()),
                "e", Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(pub.getPublicExponent().toByteArray())
        );

        return Map.of("keys", java.util.List.of(key));
    }
}