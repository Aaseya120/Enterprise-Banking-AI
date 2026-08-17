package com.bank.common.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Every downstream service (not api-gateway, which has its own JWT-issuing
 * config) gets a stateless filter chain with no login form/session and no
 * URL-pattern authorization rules -- authorization happens entirely via
 * @PreAuthorize on individual controller/service methods, driven by the
 * roles TrustedIdentityFilter put into the SecurityContext. Endpoints with
 * no @PreAuthorize annotation remain open to anyone who can reach the
 * service's port; see task.md for which endpoints currently have one.
 */
@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class DownstreamSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
