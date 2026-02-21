package no.clueless.oauth;

import io.javalin.config.JavalinConfig;
import io.javalin.config.Key;
import io.javalin.config.NoValueForKeyException;

/**
 * Helper class for OAuth security guard.
 */
public class OAuthSecurityGuard {
    /**
     * A Javalin config key indicating whether the OAuth security guard is active.
     */
    public static final Key<Boolean> GUARD_ACTIVE = new Key<>("oauth-guard-active");

    private OAuthSecurityGuard() {
    }

    /**
     * Checks if the OAuth security guard is active.
     *
     * @param config The Javalin config.
     * @throws IllegalArgumentException if config is null.
     * @throws RuntimeException         if the OAuth security guard is not active.
     */
    public static void requireActiveGuard(JavalinConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        try {
            if (!config.pvt.appDataManager.get(OAuthSecurityGuard.GUARD_ACTIVE)) {
                throw new RuntimeException("OAuth security has been explicitly disabled. Please enable OAuth security by registering the OAuth Resource Server plugin");
            }
        } catch (NoValueForKeyException e) {
            throw new RuntimeException("OAuth security is not enabled because it has not been configured. Please configure OAuth security by registering the OAuth Resource Server plugin", e);
        }
    }
}
