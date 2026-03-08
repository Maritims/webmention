package no.clueless.oauth.javalin;

import io.javalin.config.JavalinConfig;
import io.javalin.config.Key;
import io.javalin.config.NoValueForKeyException;
import org.jetbrains.annotations.NotNull;

public class OAuthSecurityGuard {
    public static final Key<Boolean> GUARD_ACTIVE = new Key<>("oauth-guard-active");

    private OAuthSecurityGuard() {
    }

    public static void requireActiveGuard(@NotNull JavalinConfig config) {
        try {
            if (!config.pvt.appDataManager.get(OAuthSecurityGuard.GUARD_ACTIVE)) {
                throw new RuntimeException("OAuth security has been explicitly disabled. Please enable OAuth security by registering the OAuth Resource Server plugin");
            }
        } catch (NoValueForKeyException e) {
            throw new RuntimeException("OAuth security is not enabled because it has not been configured. Please configure OAuth security by registering the OAuth Resource Server plugin", e);
        }
    }
}
