package no.clueless.oauth;

import io.javalin.config.JavalinConfig;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OAuthResourceServerPlugin extends Plugin<Void> {
    @NotNull
    private final TokenValidator tokenValidator;
    @NotNull
    private final String         principalKey;

    public OAuthResourceServerPlugin(@NotNull TokenValidator tokenValidator, @Nullable String principalKey) {
        this.tokenValidator = tokenValidator;
        this.principalKey   = principalKey == null || principalKey.isBlank() ? "auth_principal" : principalKey;
    }

    public OAuthResourceServerPlugin(TokenValidator tokenValidator) {
        this(tokenValidator, "auth_principal");
    }

    @Override
    public void onInitialize(@NotNull JavalinConfig config) {
        config.appData(OAuthSecurityGuard.GUARD_ACTIVE, true);

        config.router.mount(router -> router.beforeMatched(ctx -> {
            var routeRoles = ctx.routeRoles();
            if (routeRoles.isEmpty()) {
                return;
            }

            var authorizationHeader = ctx.header("Authorization");
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                throw new UnauthorizedResponse("Missing or invalid Authorization header");
            }

            var token     = authorizationHeader.substring(7);
            var principal = tokenValidator.validate(token);
            var hasAccess = principal.scopes().stream().anyMatch(routeRoles::contains);
            if (!hasAccess) {
                throw new ForbiddenResponse("You do not have access to this resource");
            }

            ctx.attribute(principalKey, principal);
        }));
    }
}
