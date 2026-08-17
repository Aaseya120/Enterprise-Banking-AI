package com.bank.gateway.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Active whenever the "keycloak" profile is NOT active (i.e. the default,
 * out-of-the-box run mode). Adding spring-boot-starter-oauth2-resource-server
 * pulls spring-boot-starter-security onto the classpath, and Spring Boot
 * auto-configures a DEFAULT reactive security chain -- requiring HTTP Basic
 * auth against a randomly generated password -- for any WebFlux app that
 * has Spring Security on the classpath but no SecurityWebFilterChain bean
 * of its own. That is the exact same silent-lockdown bug already found and
 * fixed once in this codebase (common-security's DownstreamSecurityConfig,
 * see task.md's Security section) -- this class exists purely to prevent
 * it from reappearing here now that api-gateway also has Spring Security
 * on its classpath.
 *
 * This bean permits every request at the Spring Security layer -- actual
 * authentication in demo mode is JwtAuthenticationGlobalFilter's job (a
 * Gateway GlobalFilter, which runs independently of Spring Security and
 * already returns 401 for a missing/invalid demo JWT before routing).
 */
@Configuration
@EnableWebFluxSecurity
@Profile("!keycloak")
@SuppressWarnings("null")
public class DemoModeSecurityConfig {

    @Bean
    public SecurityWebFilterChain demoModeFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchange -> exchange.anyExchange().permitAll())
                .build();
    }
}
