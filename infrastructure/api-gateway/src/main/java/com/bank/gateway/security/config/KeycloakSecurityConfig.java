package com.bank.gateway.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Real Keycloak + OAuth2 resource server validation (plan section 15/16),
 * active only with --spring.profiles.active=keycloak (or
 * SPRING_PROFILES_ACTIVE=keycloak). Standard Spring Cloud Gateway pattern:
 * ReactiveJwtDecoder is auto-configured from
 * spring.security.oauth2.resourceserver.jwt.issuer-uri (see the
 * "keycloak" profile block in application.yml) -- Spring Boot fetches
 * Keycloak's public signing keys from its JWKS endpoint at startup and on
 * key rotation, so this service never handles a shared secret for
 * verification the way the demo mode's JwtSupport does.
 *
 * Public paths mirror JwtAuthenticationGlobalFilter's demo-mode list
 * (/actuator/health always open; there is no local /api/v1/auth/** to
 * exempt here since Keycloak itself issues tokens, not this service --
 * see README for how to obtain one directly from Keycloak's token
 * endpoint).
 *
 * IMPORTANT CAVEAT: this class is reviewed for correctness against
 * Spring Security's documented reactive resource-server API, but has NOT
 * been run against a live Keycloak instance in this environment (no
 * network access to pull the Keycloak image or start a JVM here) -- see
 * task.md.
 */
@Configuration
@EnableWebFluxSecurity
@Profile("keycloak")
public class KeycloakSecurityConfig {

    @Bean
    public SecurityWebFilterChain keycloakFilterChain(ServerHttpSecurity http, ReactiveJwtDecoder jwtDecoder) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/health").permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtDecoder(jwtDecoder)))
                .build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
