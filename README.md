# Spring JWT Auth Server

A Spring Boot authentication server that issues and signs JWT tokens using RSA asymmetric encryption. 
Resource servers can validate tokens offline by fetching the public key from the standard JWKS endpoint.

## Features

- **RSA-2048 signed JWTs** — tokens are signed with a private key; resource servers verify using the public key
- **JWKS endpoint** — exposes the public key at `/.well-known/jwks.json` (RFC 7517)
- **Stateless** — no server-side sessions; all auth state lives in the token
- **Spring Security** — BCrypt password hashing, in-memory user store

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.5.x |
| Language | Java 17 |
| JWT Library | JJWT 0.12.3 |
| Security | Spring Security 6 |
| Build | Maven |

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.6+

### Run

```bash
./mvnw spring-boot:run
```

The server starts on `http://localhost:8080`.

## API Reference

### POST `/auth/login`

Authenticate a user and receive a JWT token.

**Request**
```json
{
  "username": "alice",
  "password": "password123"
}
```

**Response `200 OK`**
```json
{
  "token": "eyJhbGci...",
  "tokenType": "Bearer",
  "expiresIn": 86400000
}
```

**Response `401 Unauthorized`**
```
Invalid username or password
```

---

### GET `/.well-known/jwks.json`

Returns the RSA public key as a JSON Web Key Set. Resource servers use this to verify JWT signatures.

**Response `200 OK`**
```json
{
  "keys": [
    {
      "kty": "RSA",
      "use": "sig",
      "alg": "RS256",
      "kid": "auth-server-key-v1",
      "n": "<base64url-modulus>",
      "e": "<base64url-exponent>"
    }
  ]
}
```

## JWT Token Details

| Claim | Value |
|---|---|
| `sub` | Username |
| `roles` | List of user authorities |
| `iat` | Issued-at timestamp |
| `exp` | Expiry (issued-at + 24 hours) |
| `kid` (header) | `auth-server-key-v1` |

Signing algorithm: **RS256** (RSA + SHA-256).

## Demo Users

| Username | Password | Roles |
|---|---|---|
| `alice` | `password123` | `ROLE_USER` |
| `admin` | `admin123` | `ROLE_USER`, `ROLE_ADMIN` |

## Using the Token

Include the token in the `Authorization` header of subsequent requests to resource servers:

```
Authorization: Bearer <token>
```

Resource servers can validate the signature without calling back to this server by fetching the public key from `/.well-known/jwks.json`.

## Configuration

`src/main/resources/application.properties`:

```properties
server.port=8080
jwt.expiration=86400000
```

`jwt.expiration` is in milliseconds (default: 24 hours).

## Architecture

```
Client
  │
  ├─ POST /auth/login ──► AuthController
  │                            │
  │                     Validates credentials
  │                     (Spring Security + BCrypt)
  │                            │
  │                       JwtUtil signs token
  │                       with RSA private key
  │                            │
  └─ ◄─────────────── JWT token returned

Resource Server
  │
  └─ GET /.well-known/jwks.json ──► JwksController
                                         │
                                  Returns RSA public key
                                  for offline token
                                  verification
```

## Production Considerations

This project is intended for learning. Before using in production:

- Replace the in-memory `UserDetailsService` with a database-backed implementation
- Persist the RSA key pair (a new pair is generated on every restart, invalidating all existing tokens)
- Configure HTTPS/TLS
- Add CORS configuration if the API is browser-facing
- Implement a token refresh flow
- Add rate limiting on the login endpoint
- Write unit and integration tests
