package no.clueless.oauth.javalin;

import io.javalin.config.JavalinConfig;
import io.javalin.http.BadRequestResponse;
import io.javalin.plugin.Plugin;
import no.clueless.oauth2.core.ClientStore;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.*;

public class OAuthManagementPlugin extends Plugin<Void> {
    @NotNull
    private final ClientStore clientStore;
    private final boolean     isEnabled;

    public OAuthManagementPlugin(@NotNull ClientStore clientStore, boolean isEnabled) {
        this.clientStore = clientStore;
        this.isEnabled   = isEnabled;
    }

    @Override
    public void onInitialize(@NotNull JavalinConfig config) {
        OAuthSecurityGuard.requireActiveGuard(config);

        if (!isEnabled) {
            return;
        }

        config.router.mount(router -> {
        }).apiBuilder(() -> path("/oauth/manage", () -> path("clients", () -> {
            get("", ctx -> {
                var page    = ctx.formParamAsClass("page", Integer.class).getOrDefault(0);
                var size    = ctx.formParamAsClass("size", Integer.class).getOrDefault(10);
                var clients = clientStore.getClients(page, size, "id", false);
                if (clients.isEmpty()) {
                    ctx.status(204);
                } else {
                    ctx.json(clients);
                }
            }, Scope.CLIENTS_MANAGE);

            post("", ctx -> {
                var clientId     = ctx.formParam("clientId");
                var clientSecret = ctx.formParam("clientSecret");
                var scopes       = ctx.formParam("scopes");

                if (clientId == null || clientId.isBlank()) {
                    throw new BadRequestResponse("Missing clientId");
                }
                if (clientSecret == null || clientSecret.isBlank()) {
                    throw new BadRequestResponse("Missing clientSecret");
                }
                if (scopes == null || scopes.isBlank()) {
                    throw new BadRequestResponse("Missing scopes");
                }

                clientStore.registerClient(clientId, clientSecret, Arrays.stream(scopes.split(",")).collect(Collectors.toSet()));
                ctx.status(201);
            }, Scope.CLIENTS_MANAGE);

            delete("{clientId}", ctx -> {
                var clientId = ctx.pathParam("clientId");
                if (clientId.isBlank()) {
                    throw new BadRequestResponse("clientId cannot be null or blank");
                }
                clientStore.deleteClient(clientId);
                ctx.status(204);
            }, Scope.CLIENTS_MANAGE);

            patch("{clientId}/enable", ctx -> {
                var clientId = ctx.pathParam("clientId");
                if (clientId.isBlank()) {
                    throw new BadRequestResponse("clientId cannot be null or blank");
                }
                clientStore.enableClient(clientId);
                ctx.status(204);
            }, Scope.CLIENTS_MANAGE);

            patch("{clientId}/disable", ctx -> {
                var clientId = ctx.pathParam("clientId");
                if (clientId.isBlank()) {
                    throw new BadRequestResponse("clientId cannot be null or blank");
                }
                clientStore.disableClient(clientId);
                ctx.status(204);
            }, Scope.CLIENTS_MANAGE);
        })));
    }
}
