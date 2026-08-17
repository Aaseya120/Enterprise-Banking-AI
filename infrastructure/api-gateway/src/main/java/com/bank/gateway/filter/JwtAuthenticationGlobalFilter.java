package com.bank.gateway.filter;

import com.bank.gateway.security.JwtSupport;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

/**
 * Active only when the "keycloak" profile is NOT active -- see
 * KeycloakSecurityConfig for the real-Keycloak equivalent of this filter's
 * job (identity verification + propagation), which takes over under
 * --spring.profiles.active=keycloak.
 *
 * The platform's trust boundary (plan section 15/16). For every request:
 *  1. Public paths (/api/v1/auth/**, /actuator/health) pass straight through.
 *  2. Everything else MUST carry a valid "Authorization: Bearer <jwt>" --
 *     missing or invalid -> 401, before the request ever reaches a backend.
 *  3. Any X-User-Id / X-User-Roles the caller sent themselves is stripped
 *     first (plan section 15: "Do not trust user identity headers from
 *     external clients"), then replaced with values derived from the
 *     verified JWT claims -- this is what makes it safe for
 *     TrustedIdentityFilter in each downstream service to trust those
 *     headers unconditionally.
 */
@Component
@Profile("!keycloak")
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PREFIXES = List.of("/api/v1/auth/", "/actuator/health");
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLES_HEADER = "X-User-Roles";

    private final JwtSupport jwtSupport;

    public JwtAuthenticationGlobalFilter(JwtSupport jwtSupport) {
        this.jwtSupport = jwtSupport;
    }

    @Override
    public int getOrder() {
        return -1; // run before routing
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing Authorization header");
        }

        String token = authHeader.substring("Bearer ".length());
        try {
            Claims claims = jwtSupport.verify(token);
            Set<String> roles = JwtSupport.rolesOf(claims);

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .headers(headers -> {
                        headers.remove(USER_ID_HEADER);
                        headers.remove(USER_ROLES_HEADER);
                        headers.set(USER_ID_HEADER, claims.getSubject());
                        headers.set(USER_ROLES_HEADER, String.join(",", roles));
                    })
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (JwtException e) {
            return unauthorized(exchange, "Invalid or expired token");
        }
    }

    private boolean isPublic(String path) {
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String reason) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("X-Auth-Error", reason);
        return response.setComplete();
    }
}
