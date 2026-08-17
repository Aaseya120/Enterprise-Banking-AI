package com.bank.gateway.controller;

import com.bank.gateway.dto.LoginRequest;
import com.bank.gateway.security.DemoUserStore;
import com.bank.gateway.security.JwtSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * Public endpoint (plan section 15: "/api/v1/auth/** ... Public
 * endpoints"). Issues a JWT for one of the five demo users in
 * DemoUserStore -- see that class's javadoc for why this exists instead of
 * real Keycloak/OIDC. Try: POST {"username":"staff1","password":"password"}.
 *
 * Active only when the "keycloak" profile is NOT active -- under
 * --spring.profiles.active=keycloak, get a token directly from Keycloak's
 * own token endpoint instead (see README).
 */
@RestController
@RequestMapping("/api/v1/auth")
@Profile("!keycloak")
public class AuthController {

    private final JwtSupport jwtSupport;
    private final long expiryMinutes;

    public AuthController(JwtSupport jwtSupport,
                           @Value("${bank.security.jwt.expiry-minutes:60}") long expiryMinutes) {
        this.jwtSupport = jwtSupport;
        this.expiryMinutes = expiryMinutes;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginRequest.Response> login(@RequestBody LoginRequest request) {
        Set<String> roles = DemoUserStore.authenticate(request.username(), request.password());
        if (roles == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = jwtSupport.issueToken(request.username(), roles);
        return ResponseEntity.ok(new LoginRequest.Response(token, "Bearer", expiryMinutes));
    }
}
