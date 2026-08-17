package com.bank.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * The Keycloak-mode equivalent of JwtAuthenticationGlobalFilter's identity
 * propagation step. By the time this filter runs, KeycloakSecurityConfig's
 * SecurityWebFilterChain has already rejected any request with a missing
 * or invalid token (this filter only ever sees authenticated requests) --
 * its only job is translating Keycloak's JWT claim shape into the same
 * X-User-Id / X-User-Roles headers the demo flow produces, so every
 * downstream service's TrustedIdentityFilter works identically regardless
 * of which auth mode issued the token.
 *
 * Keycloak puts realm-level roles under a nested claim,
 * {@code realm_access.roles} (a JSON object containing a "roles" array) --
 * NOT a flat "roles" claim like the demo JWTs use, which is why this needs
 * its own extraction logic rather than reusing JwtSupport.rolesOf().
 */
@Component
@Profile("keycloak")
public class KeycloakIdentityPropagationFilter implements GlobalFilter, Ordered {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    public int getOrder() {
        return 0; // after KeycloakSecurityConfig's authentication (which runs as part of the WebFilter chain), before routing
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal())
                .cast(Jwt.class)
                .flatMap(jwt -> {
                    String userId = jwt.getClaimAsString("preferred_username") != null
                            ? jwt.getClaimAsString("preferred_username") : jwt.getSubject();

                    Map<String, Object> realmAccess = jwt.getClaim("realm_access");
                    List<String> roles = realmAccess != null
                            ? (List<String>) realmAccess.getOrDefault("roles", List.of())
                            : List.of();

                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .headers(headers -> {
                                headers.remove(USER_ID_HEADER);
                                headers.remove(USER_ROLES_HEADER);
                                headers.set(USER_ID_HEADER, userId);
                                headers.set(USER_ROLES_HEADER, String.join(",", roles));
                            })
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                // Public paths (e.g. /actuator/health) have no authenticated principal --
                // fall through unchanged rather than erroring on an empty security context.
                .switchIfEmpty(chain.filter(exchange));
    }
}
