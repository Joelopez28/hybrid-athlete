package com.ruky.training.security;

import java.util.Optional;
import java.util.UUID;

/** Request-scoped principal populated by the JWT filter. */
public final class AuthContext {

    private static final ThreadLocal<Principal> CURRENT = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(UUID athleteId, String token, String role) {
        CURRENT.set(new Principal(athleteId, token, role));
    }

    public static Optional<Principal> principal() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static Principal require() {
        Principal principal = CURRENT.get();
        if (principal == null) {
            throw new IllegalStateException("No authenticated athlete in context");
        }
        return principal;
    }

    public static String token() {
        Principal principal = CURRENT.get();
        return principal == null ? null : principal.token();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record Principal(UUID athleteId, String token, String role) {
    }
}
