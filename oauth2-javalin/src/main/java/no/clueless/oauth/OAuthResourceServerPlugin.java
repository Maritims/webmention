package no.clueless.oauth;

import io.javalin.config.JavalinConfig;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

public class OAuthResourceServerPlugin<Principal> extends Plugin<OAuthResourceServerPlugin.Config<Principal>> {
    public static class Config<Principal> {
        public TokenValidator<Principal> tokenValidator;
        public ScopeExtractor<Principal> scopeExtractor;
        public String                    principalKey = "auth_principal";
    }

    public OAuthResourceServerPlugin(@Nullable Consumer<Config<Principal>> userConfig) {
        super(userConfig, new Config<>());
    }

    @Override
    public void onInitialize(@NotNull JavalinConfig config) {
        config.appData(OAuthSecurity.GUARD_ACTIVE, true);

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
            var principal = Optional.ofNullable(pluginConfig.tokenValidator.validate(token)).orElseThrow(() -> new UnauthorizedResponse("Invalid token"));
            var scopes    = pluginConfig.scopeExtractor.extractScopes(principal);
            var hasAccess = scopes.stream().anyMatch(routeRoles::contains);
            if (!hasAccess) {
                throw new ForbiddenResponse("You do not have access to this resource");
            }

            ctx.attribute(pluginConfig.principalKey, principal);
        }));
    }
}
