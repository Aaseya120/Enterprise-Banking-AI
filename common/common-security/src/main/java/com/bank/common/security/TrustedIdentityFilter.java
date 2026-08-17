package com.bank.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Reads X-User-Id / X-User-Roles (set by api-gateway after verifying the
 * caller's JWT -- see SecurityHeaders javadoc) and populates the Spring
 * Security context so @PreAuthorize("hasRole('BANK_STAFF')") works in
 * individual services without each one re-validating a JWT.
 *
 * This filter trusts the headers unconditionally -- it does NOT verify a
 * signature, because by the time a request reaches here the gateway
 * already did. That trust is only sound if nothing but the gateway can
 * reach this service's port directly, which this repo does not enforce
 * (no NetworkPolicy/mTLS -- see task.md). A request that arrives with no
 * X-User-Roles header (e.g. a direct call bypassing the gateway, as every
 * example in this repo's README does today) is left unauthenticated, so
 * @PreAuthorize-protected endpoints will correctly reject it.
 */
@Component
@Order(2)
@SuppressWarnings("null")
public class TrustedIdentityFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest request, @org.springframework.lang.NonNull HttpServletResponse response, @org.springframework.lang.NonNull FilterChain chain)
            throws ServletException, IOException {
        String userId = request.getHeader(SecurityHeaders.USER_ID);
        String rolesHeader = request.getHeader(SecurityHeaders.USER_ROLES);

        if (userId != null && rolesHeader != null && !rolesHeader.isBlank()) {
            List<GrantedAuthority> authorities = Arrays.stream(rolesHeader.split(","))
                    .map(String::trim)
                    .filter(r -> !r.isEmpty())
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .map(GrantedAuthority.class::cast)
                    .toList();

            var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
