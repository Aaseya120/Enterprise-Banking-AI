package com.bank.common.security;

/**
 * Header names used to propagate a verified identity from api-gateway to
 * every downstream service, once the gateway has validated the caller's
 * JWT (plan section 15/16: "Only trusted gateway/service infrastructure
 * may propagate authenticated identity"). Downstream services never see or
 * validate a JWT themselves in this scaffold -- they trust these headers,
 * which is only safe if network topology genuinely prevents anything but
 * the gateway from reaching them directly. This repo does NOT enforce that
 * network isolation (no NetworkPolicy/mTLS) -- see task.md.
 */
public final class SecurityHeaders {
    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLES = "X-User-Roles";

    private SecurityHeaders() {
    }
}
