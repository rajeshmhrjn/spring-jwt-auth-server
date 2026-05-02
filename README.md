# Spring JWT Auth Server

A Spring Boot authentication server that issues and signs JWT access tokens using RSA asymmetric encryption, paired with opaque refresh tokens for long-lived sessions. Resource servers can validate access tokens offline by fetching the public key from the standard JWKS endpoint.

## Features

- **RSA-2048 signed access tokens** — signed with a private key; resource servers verify using the public key (RS256)
- **Refresh token support** — opaque UUID tokens with a 7-day TTL; rotated on every use
- **Token rotation** — each refresh call revokes the old refresh token and issues a new one, limiting exposure from stolen tokens
- **JWKS endpoint** — exposes the public key at `/.well-known/jwks.json` (RFC 7517)
- **Logout** — revokes the refresh token server-side; the access token expires naturally
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

Authenticate a user and receive an access token + refresh token.

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
  "accessToken": "eyJhbGci...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "tokenType": "Bearer",
  "expiresIn": 900000
}
```

**Response `401 Unauthorized`**
```
Invalid username or password
```

---

### POST `/auth/refresh`

Exchange a valid refresh token for a new access token. Both the access token and the refresh token are rotated — the old refresh token is revoked immediately.

**Request**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response `200 OK`** — same shape as `/auth/login`
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "661f9511-f30c-52e5-b827-557766551111",
  "tokenType": "Bearer",
  "expiresIn": 900000
}
```

**Response `401 Unauthorized`**
```
Refresh token not found
```
or
```
Refresh token has expired. Please log in again.
```

---

### POST `/auth/logout`

Revoke the refresh token. The access token will expire on its own (after `jwt.expiration` ms).

**Request**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response `200 OK`**
```
Logged out successfully
```

---

### GET `/.well-known/jwks.json`

Returns the RSA public key as a JSON Web Key Set. Resource servers use this to verify JWT signatures offline.

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

## Token Details

### Access Token (JWT)

Short-lived, RS256-signed JWT. Validated by resource servers without calling back to this server.

| Claim | Value |
|---|---|
| `sub` | Username |
| `roles` | List of user authorities |
| `iat` | Issued-at timestamp |
| `exp` | Expiry (iat + `jwt.expiration` ms) |
| `kid` (header) | `auth-server-key-v1` |

Signing algorithm: **RS256** (RSA + SHA-256).

### Refresh Token (Opaque)

Long-lived, random UUID stored in server memory. Used only to obtain a new access token via `/auth/refresh`. Rotated on every use.

| Property | Value |
|---|---|
| Format | Random UUID |
| TTL | 7 days (`jwt.refresh-expiration`) |
| Storage | In-memory `ConcurrentHashMap` (demo) |
| Rotation | Yes — old token revoked on each refresh |

## Demo Users

| Username | Password | Roles |
|---|---|---|
| `alice` | `password123` | `ROLE_USER` |
| `admin` | `admin123` | `ROLE_USER`, `ROLE_ADMIN` |

## Using the Access Token

Include the access token in the `Authorization` header of requests to resource servers:

```
Authorization: Bearer <accessToken>
```

Resource servers validate the signature offline by fetching the public key from `/.well-known/jwks.json`.

## Configuration

`src/main/resources/application.properties`:

```properties
server.port=8080

# Access token TTL in milliseconds (15 minutes)
jwt.expiration=900000

# Refresh token TTL in milliseconds (7 days)
jwt.refresh-expiration=604800000
```

## Architecture

```
Client
  │
  ├─ POST /auth/login ──────────────► AuthController
  │                                        │
  │                                 Validates credentials
  │                                 (Spring Security + BCrypt)
  │                                        │
  │                                 JwtUtil signs access token (RS256)
  │                                 RefreshTokenService creates refresh token
  │                                        │
  └─ ◄──────────────── { accessToken, refreshToken }

  │
  ├─ POST /auth/refresh ────────────► AuthController
  │    { refreshToken }                    │
  │                                 RefreshTokenService validates + revokes old token
  │                                 Issues new refresh token (rotation)
  │                                 JwtUtil signs new access token
  │                                        │
  └─ ◄──────────────── { accessToken, refreshToken }

  │
  ├─ POST /auth/logout ────────────► AuthController
  │    { refreshToken }                    │
  │                                 RefreshTokenService.revokeRefreshToken()
  │                                 (access token expires naturally)
  │                                        │
  └─ ◄──────────────── 200 OK

Resource Server
  │
  └─ GET /.well-known/jwks.json ──► JwksController
                                         │
                                  Returns RSA public key
                                  for offline token verification
```

## Production Considerations

This project is intended for learning. Before using in production:

- Replace the in-memory `UserDetailsService` with a database-backed implementation
- Replace the in-memory refresh token store with a JPA-backed repository (see schema comment in `RefreshTokenService`)
- Persist the RSA key pair (a new pair is generated on every restart, invalidating all existing access tokens)
- Configure HTTPS/TLS
- Add CORS configuration if the API is browser-facing
- Tune `jwt.expiration` to your security requirements (current: 15 minutes)
- Add rate limiting on the login endpoint
- Write unit and integration tests
