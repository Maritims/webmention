package no.clueless.oauth.javalin;

import io.javalin.config.JavalinConfig;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.plugin.Plugin;
import no.clueless.oauth2.core.ClientStore;
import no.clueless.oauth2.core.TokenGenerator;
import no.clueless.oauth2.core.TokenResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.*;

public class OAuthServerPlugin extends Plugin<Void> {
    private static final Logger log = LoggerFactory.getLogger(OAuthServerPlugin.class);

    @NotNull
    private final String         tokenPath;
    @NotNull
    private final ClientStore    clientStore;
    @NotNull
    private final TokenGenerator tokenGenerator;
    private final long           accessTokenValiditySeconds;

    public OAuthServerPlugin(@Nullable String tokenPath, long accessTokenValidityInSeconds, @NotNull ClientStore clientStore, @NotNull TokenGenerator tokenGenerator) {
        this.tokenPath                  = tokenPath == null || tokenPath.isBlank() ? "/oauth/token" : tokenPath;
        this.accessTokenValiditySeconds = accessTokenValidityInSeconds < 1 ? 3600 : accessTokenValidityInSeconds;
        this.clientStore                = clientStore;
        this.tokenGenerator             = tokenGenerator;
    }

    public OAuthServerPlugin(@NotNull ClientStore clientStore, @NotNull TokenGenerator tokenGenerator) {
        this("/oauth/token", 3600, clientStore, tokenGenerator);
    }

    @Override
    public void onInitialize(@NotNull JavalinConfig config) {
        if (clientStore.shouldSeedInitialClient() && clientStore.hasSeedCredentials()) {
            log.debug("Seeding initial client");
            clientStore.seedInitialClient();
        } else if (clientStore.shouldSeedInitialClient()) {
            throw new IllegalStateException("Seeding initial client is enabled but no seed credentials have been provided. Please set WEBMENTION_INITIAL_CLIENT_ID, WEBMENTION_INITIAL_CLIENT_SECRET and WEBMENTION_INITIAL_CLIENT_SCOPES to seed the initial client");
        }

        config.router.mount(router -> {
        }).apiBuilder(() -> post(tokenPath, ctx -> {
            var grantType = ctx.formParam("grant_type");
            if (!"client_credentials".equals(grantType)) {
                throw new BadRequestResponse("unsupported_grant_type");
            }

            var clientId     = ctx.formParam("client_id");
            var clientSecret = ctx.formParam("client_secret");

            var authorizationHeader = ctx.header("Authorization");
            if (authorizationHeader != null && authorizationHeader.startsWith("Basic ")) {
                var base64EncodedToken = authorizationHeader.substring(6);
                var decodedToken       = new String(Base64.getDecoder().decode(base64EncodedToken));
                var parts              = decodedToken.split(":", 2);
                clientId     = parts[0];
                clientSecret = parts[1];
            }

            if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank() || !clientStore.authenticate(clientId, clientSecret)) {
                throw new UnauthorizedResponse("invalid_client");
            }

            var client = clientStore.getClient(clientId);
            if (client == null || !client.isEnabled()) {
                throw new BadRequestResponse("invalid_client");
            }

            Set<Scope> finalScopes;
            var        scopeParameter = ctx.formParam("scope");
            if (scopeParameter == null || scopeParameter.isBlank()) {
                finalScopes = client.scopes()
                        .stream()
                        .map(Scope::fromString)
                        .flatMap(Optional::stream)
                        .collect(Collectors.toSet());
            } else {
                var requestedLabels = Arrays.stream(scopeParameter.split("\\s+")).collect(Collectors.toSet());
                finalScopes = requestedLabels.stream()
                        .map(Scope::fromString)
                        .flatMap(Optional::stream)
                        .collect(Collectors.toSet());

                if (finalScopes.size() != requestedLabels.size() || !client.scopes().containsAll(requestedLabels)) {
                    throw new BadRequestResponse("invalid_scope");
                }
            }

            var token = tokenGenerator.generate(client, finalScopes.stream().map(Scope::getLabel).collect(Collectors.toSet()));
            ctx.json(new TokenResponse(
                    token,
                    "Bearer",
                    accessTokenValiditySeconds,
                    finalScopes.stream().map(Scope::getLabel).collect(Collectors.joining(""))));
        }));
    }
}
