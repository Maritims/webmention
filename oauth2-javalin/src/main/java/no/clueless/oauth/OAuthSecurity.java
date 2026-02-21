package no.clueless.oauth;

import io.javalin.config.Key;

public class OAuthSecurity {
    public static final Key<Boolean> GUARD_ACTIVE = new Key<>("oauth-guard-active");
}
