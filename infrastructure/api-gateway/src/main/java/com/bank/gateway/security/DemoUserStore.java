package com.bank.gateway.security;

import java.util.Map;
import java.util.Set;

/**
 * In-memory demo credential store. NOT how a real deployment authenticates
 * users -- plan section 16 calls for Keycloak/OIDC. This exists so the
 * platform's JWT + RBAC flow is runnable and testable without standing up
 * an identity provider. Passwords are plaintext here deliberately (this is
 * a fixed demo fixture, never real user data) -- a real user store must
 * hash with BCrypt/Argon2, per plan section 16.
 */
public final class DemoUserStore {

    private record DemoUser(String password, Set<String> roles) {
    }

    private static final Map<String, DemoUser> USERS = Map.of(
            "customer1", new DemoUser("password", Set.of("CUSTOMER")),
            "staff1", new DemoUser("password", Set.of("BANK_STAFF")),
            "analyst1", new DemoUser("password", Set.of("ANALYST")),
            "compliance1", new DemoUser("password", Set.of("COMPLIANCE_OFFICER")),
            "admin1", new DemoUser("password", Set.of("ADMIN"))
    );

    private DemoUserStore() {
    }

    public static Set<String> authenticate(String username, String password) {
        DemoUser user = USERS.get(username);
        if (user == null || !user.password().equals(password)) {
            return null;
        }
        return user.roles();
    }
}
