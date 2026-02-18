package no.clueless.webmention.javalin;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.function.Consumer;

public class OAuth2Plugin extends Plugin<OAuth2Config> {
    @NotNull
    @Override
    public String name() {
        return "Clueless OAuth2 Plugin";
    }

    public OAuth2Plugin(@Nullable Consumer<OAuth2Config> userConfig) {
        super(userConfig, new OAuth2Config());
    }

    /**
     * Adds support for the OAuth2 token endpoint.
     *
     * @param config the Javalin config
     */
    @Override
    public void onStart(@NotNull JavalinConfig config) {
        config.router.mount(router -> router.post("/oauth/token", this::handleTokenRequest));
    }

    /**
     * Handles the token request for a client credentials grant. Supports extracting client id and secret from either the Authorization header or from the form parameters.
     *
     * @param ctx the Javalin context
     */
    protected void handleTokenRequest(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("ctx cannot be null");
        }

        String clientId;
        String clientSecret;

        var authorizationHeader = ctx.header("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            var base64EncodedToken = authorizationHeader.substring(7);
            var decodedToken       = new String(Base64.getDecoder().decode(base64EncodedToken));
            var parts              = decodedToken.split(":");
            clientId     = parts[0];
            clientSecret = parts[1];
        } else {
            clientId     = ctx.formParam("client_id");
            clientSecret = ctx.formParam("client_secret");
        }

        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank() || !pluginConfig.getClientValidator().isValidClient(clientId, clientSecret)) {
            throw new UnauthorizedResponse("Invalid credentials");
        }

        var token = JWT.create()
                .withIssuer(pluginConfig.getIssuer())
                .withSubject(clientId)
                .withExpiresAt(new Date(System.currentTimeMillis() + (pluginConfig.getAccessTokenValiditySeconds()) * 1000))
                .sign(Algorithm.HMAC256(pluginConfig.getJwtSecret()));

        ctx.json(Map.of(
                "access_token", token,
                "token_type", "Bearer",
                "expires_in", pluginConfig.getAccessTokenValiditySeconds()
        ));
    }
}
