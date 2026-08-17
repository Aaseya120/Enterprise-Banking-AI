package com.bank.common.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * HS256 JWT issue/verify, shared between api-gateway (issues on
 * /api/v1/auth/login, validates on every protected route) and any other
 * service that needs to validate a token directly.
 *
 * IMPORTANT (plan section 16): this is a deliberately simple stand-in for
 * Keycloak/OIDC, not a replacement for it. It uses a single shared secret
 * (HS256) rather than an OIDC provider's rotating signing keys, and there
 * is no user store beyond the in-memory list in api-gateway's
 * DemoUserStore. Swapping to real Keycloak later means: replace this class
 * with spring-boot-starter-oauth2-resource-server's JWT decoder pointed at
 * Keycloak's issuer-uri (already on the classpath, just unused), and delete
 * DemoUserStore/AuthController.
 */
public class JwtSupport {

    private final SecretKey key;
    private final long expiryMinutes;

    public JwtSupport(String secret, long expiryMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        this.expiryMinutes = expiryMinutes;
    }

    public String issueToken(String subject, Set<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiryMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    /** Throws JwtException (expired, malformed, bad signature) if the token is not valid. */
    public Claims verify(String token) throws JwtException {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    @SuppressWarnings("unchecked")
    public static Set<String> rolesOf(Claims claims) {
        List<String> roles = claims.get("roles", List.class);
        return roles == null ? Set.of() : roles.stream().collect(Collectors.toSet());
    }
}
