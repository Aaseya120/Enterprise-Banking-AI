package com.bank.gateway.config;

import com.bank.gateway.security.JwtSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!keycloak")
public class JwtConfig {

    @Bean
    public JwtSupport jwtSupport(
            @Value("${bank.security.jwt.secret}") String secret,
            @Value("${bank.security.jwt.expiry-minutes:60}") long expiryMinutes) {
        return new JwtSupport(secret, expiryMinutes);
    }
}
