package no.clueless.oauth;

import io.javalin.config.JavalinConfig;
import io.javalin.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

public class OAuthServerPlugin extends Plugin<OAuthServerPlugin.Config> {
    public static class Config {
        public String         tokenPath = "/oauth/token";
        public ClientStore    clientStore;
        public TokenGenerator tokenGenerator;
    }

    public OAuthServerPlugin(@Nullable Consumer<Config> userConfig) {
        super(userConfig, new Config());
    }

    @Override
    public void onInitialize(@NotNull JavalinConfig config) {
        config.router.mount(router -> router.post(pluginConfig.tokenPath, ctx -> {
            var clientId     = ctx.formParam("client_id");
            var clientSecret = ctx.formParam("client_secret");
            var grantType    = ctx.formParam("grant_type");

            var authorizationHeader = ctx.header("Authorization");
            if (authorizationHeader != null && authorizationHeader.startsWith("Basic ")) {
                var base64EncodedToken = authorizationHeader.substring(6);
                var decodedToken       = new String(java.util.Base64.getDecoder().decode(base64EncodedToken));
                var parts              = decodedToken.split(":");
                clientId     = parts[0];
                clientSecret = parts[1];
            }

            if (!"client_credentials".equals(grantType)) {
                ctx.status(400).json(Map.of(
                        "error", "unsupported_grant_type",
                        "error_description", "Only client_credentials grant type is supported"
                ));
                return;
            }

            if (pluginConfig.clientStore.authenticate(clientId, clientSecret)) {
                var client = pluginConfig.clientStore.getClient(clientId);
                if (client == null) {
                    ctx.status(401).json(Map.of("error", "invalid_client"));
                    return;
                }

                var token = pluginConfig.tokenGenerator.generate(client);
                ctx.json(Map.of(
                        "access_token", token,
                        "token_type", "Bearer",
                        "expires_in", 3600
                ));
            } else {
                ctx.status(401).json(Map.of("error", "invalid_client"));
            }
        }));
    }
}
