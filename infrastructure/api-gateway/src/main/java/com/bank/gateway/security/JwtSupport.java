package com.bank.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * INTENTIONAL DUPLICATE of com.bank.common.security.jwt.JwtSupport in the
 * common-security module. api-gateway cannot depend on common-security
 * because common-security pulls in spring-boot-starter-web (servlet stack),
 * which conflicts with Spring Cloud Gateway's requirement to run on
 * WebFlux (reactive stack) -- having both on the classpath breaks Spring
 * Boot's web-application-type autoconfiguration. If you change the JWT
 * claim structure or signing algorithm, change it in BOTH places, or
 * extract a third, dependency-free module (e.g. common-jwt) that both
 * common-security and api-gateway can depend on without pulling in a web
 * starter.
 */
public class JwtSupport {

    private final SecretKey key;
    private final long expiryMinutes;

    public JwtSupport(String secret, long expiryMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
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

    public Claims verify(String token) throws JwtException {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    @SuppressWarnings("unchecked")
    public static Set<String> rolesOf(Claims claims) {
        List<String> roles = claims.get("roles", List.class);
        return roles == null ? Set.of() : roles.stream().collect(Collectors.toSet());
    }
}
