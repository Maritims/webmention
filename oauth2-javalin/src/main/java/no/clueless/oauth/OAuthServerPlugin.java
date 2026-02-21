package no.clueless.oauth;

import io.javalin.config.JavalinConfig;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A Javalin plugin acting as an OAuth 2.0 Authorization Server.
 */
public class OAuthServerPlugin extends Plugin<OAuthServerPlugin.Config> {
    private static final Logger log = LoggerFactory.getLogger(OAuthServerPlugin.class);

    /**
     * Configuration for the OAuth server plugin.
     */
    public static class Config {
        /**
         * The path to the token endpoint. Defaults to "/oauth/token".
         */
        public String         tokenPath           = "/oauth/token";
        /**
         * The client store.
         */
        public ClientStore    clientStore;
        /**
         * The token generator.
         */
        public TokenGenerator tokenGenerator;
        public String         initialClientId     = System.getenv("WEBMENTION_INITIAL_CLIENT_ID");
        public String         initialClientSecret = System.getenv("WEBMENTION_INITIAL_CLIENT_SECRET");
        public String         initialClientScopes = System.getenv("WEBMENTION_INITIAL_CLIENT_SCOPES");

    }

    public OAuthServerPlugin(@Nullable Consumer<Config> userConfig) {
        super(userConfig, new Config());
    }

    @Override
    public void onInitialize(@NotNull JavalinConfig config) {
        if (pluginConfig.clientStore.shouldSeedInitialClient()) {
            log.debug("Seeding initial client");

            if (pluginConfig.initialClientId == null || pluginConfig.initialClientId.isBlank()) {
                throw new RuntimeException("Unable to seed initial client: Missing environment variable WEBMENTION_INITIAL_CLIENT_ID");
            }
            if (pluginConfig.initialClientSecret == null || pluginConfig.initialClientSecret.isBlank()) {
                throw new RuntimeException("Unable to seed initial client: Missing environment variable WEBMENTION_INITIAL_CLIENT_SECRET");
            }
            if (pluginConfig.initialClientScopes == null || pluginConfig.initialClientScopes.isBlank()) {
                throw new RuntimeException("Unable to seed initial client: Missing environment variable WEBMENTION_INITIAL_CLIENT_SCOPES");
            }

            var initialClientScopes = Arrays.stream(pluginConfig.initialClientScopes.trim().split("\\s+"))
                    .map(Scope::fromLabel)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toSet());
            pluginConfig.clientStore.seedInitialClient(pluginConfig.initialClientId, pluginConfig.initialClientSecret, initialClientScopes);
        } else {
            if (pluginConfig.initialClientId != null) {
                throw new RuntimeException("Potential security risk detected! WEBMENTION_INITIAL_CLIENT_ID is set but client seeding is not needed. Unset WEBMENTION_INITIAL_CLIENT_ID and restart the application");
            }
            if (pluginConfig.initialClientSecret != null) {
                throw new RuntimeException("Potential security risk detected! WEBMENTION_INITIAL_CLIENT_SECRET is set but client seeding is not needed. Unset WEBMENTION_INITIAL_CLIENT_SECRET and restart the application");
            }
            if (pluginConfig.initialClientScopes != null) {
                throw new RuntimeException("Potential security risk detected! WEBMENTION_INITIAL_CLIENT_SCOPES is set but client seeding is not needed. Unset WEBMENTION_INITIAL_CLIENT_SCOPES and restart the application");
            }
        }

        config.router.mount(router -> router.post(pluginConfig.tokenPath, ctx -> {
            var grantType = ctx.formParam("grant_type");
            if (!"client_credentials".equals(grantType)) {
                throw new BadRequestResponse("unsupported_grant_type");
            }

            var clientId     = ctx.formParam("client_id");
            var clientSecret = ctx.formParam("client_secret");

            var authorizationHeader = ctx.header("Authorization");
            if (authorizationHeader != null && authorizationHeader.startsWith("Basic ")) {
                var base64EncodedToken = authorizationHeader.substring(6);
                var decodedToken       = new String(java.util.Base64.getDecoder().decode(base64EncodedToken));
                var parts              = decodedToken.split(":", 2);
                clientId     = parts[0];
                clientSecret = parts[1];
            }

            if (!pluginConfig.clientStore.authenticate(clientId, clientSecret)) {
                throw new UnauthorizedResponse("invalid_client");
            }

            var client = Optional.ofNullable(pluginConfig.clientStore.getClient(clientId))
                    .filter(OAuthClient::isEnabled)
                    .orElseThrow(() -> new BadRequestResponse("invalid_client"));

            Set<Scope> finalScopes;
            var        scopeParameter = ctx.formParam("scope");
            if (scopeParameter == null || scopeParameter.isBlank()) {
                finalScopes = client.scopes().stream().map(Scope::fromLabel).flatMap(Optional::stream).collect(Collectors.toSet());
            } else {
                var requestedLabels = Arrays.stream(scopeParameter.split("\\s+")).collect(Collectors.toSet());
                finalScopes = requestedLabels.stream()
                        .map(Scope::fromLabel)
                        .flatMap(Optional::stream)
                        .collect(Collectors.toSet());

                if (finalScopes.size() != requestedLabels.size() || !client.scopes().containsAll(requestedLabels)) {
                    throw new BadRequestResponse("invalid_scope");
                }
            }

            var token = pluginConfig.tokenGenerator.generate(client, finalScopes);
            ctx.json(Map.of(
                    "access_token", token,
                    "token_type", "Bearer",
                    "expires_in", 3600,
                    "scope", finalScopes.stream().map(Scope::getLabel).collect(Collectors.joining(" "))
            ));
        }));
    }
}
