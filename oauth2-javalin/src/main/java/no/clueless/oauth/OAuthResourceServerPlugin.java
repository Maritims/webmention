package no.clueless.oauth;

import io.javalin.config.JavalinConfig;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A Javalin plugin acting as an OAuth 2.0 Resource Server.
 * @param <Principal> the type of the principal
 */
public class OAuthResourceServerPlugin<Principal> extends Plugin<Void> {
    private final TokenValidator<Principal> tokenValidator;
    private final ScopeExtractor<Principal> scopeExtractor;
    private final String                    principalKey;

    /**
     * Constructor.
     *
     * @param tokenValidator the token validator
     * @param scopeExtractor the scope extractor
     * @param principalKey   the principal key. Defaults to "auth_principal"
     * @throws IllegalArgumentException if tokenValidator or scopeExtractor is null
     */
    public OAuthResourceServerPlugin(TokenValidator<Principal> tokenValidator, ScopeExtractor<Principal> scopeExtractor, String principalKey) {
        if (tokenValidator == null) {
            throw new IllegalArgumentException("tokenValidator cannot be null");
        }
        if (scopeExtractor == null) {
            throw new IllegalArgumentException("scopeExtractor cannot be null");
        }
        this.tokenValidator = tokenValidator;
        this.scopeExtractor = scopeExtractor;
        this.principalKey   = principalKey == null || principalKey.isBlank() ? "auth_principal" : principalKey;
    }

    /**
     * Constructor with a default principal key.
     *
     * @param tokenValidator the token validator
     * @param scopeExtractor the scope extractor
     * @see #OAuthResourceServerPlugin(TokenValidator, ScopeExtractor, String)
     */
    public OAuthResourceServerPlugin(TokenValidator<Principal> tokenValidator, ScopeExtractor<Principal> scopeExtractor) {
        this(tokenValidator, scopeExtractor, "auth_principal");
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
            var principal = Optional.ofNullable(tokenValidator.validate(token)).orElseThrow(() -> new UnauthorizedResponse("Invalid token"));
            var scopes    = scopeExtractor.extractScopes(principal);
            var hasAccess = scopes.stream().anyMatch(routeRoles::contains);
            if (!hasAccess) {
                throw new ForbiddenResponse("You do not have access to this resource");
            }

            ctx.attribute(principalKey, principal);
        }));
    }
}
