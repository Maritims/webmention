package no.clueless.webmention.cli;

import no.clueless.webmention.api_client.WebmentionManagementApiClient;
import org.jetbrains.annotations.NotNull;

import java.net.URI;

@Command(
        name = "publish",
        description = "Publishes a webmention by its id.",
        parameters = {
                @CommandParameter(longName = "uri", shortName = "u", description = "The base API URI.", requiresValue = true, required = true, type = URI.class),
                @CommandParameter(longName = "token-endpoint", shortName = "t", description = "The token endpoint URI.", requiresValue = true, defaultValue = "/oauth/token", type = String.class),
                @CommandParameter(longName = "management-endpoint", shortName = "m", description = "The webmention management endpoint URI.", requiresValue = true, defaultValue = "/webmention/manage", type = String.class),
                @CommandParameter(longName = "id", shortName = "i", description = "The webmention id.", requiresValue = true, required = true, type = Integer.class)
        }
)
public class PublishWebmentionCommand extends CommandBase {
    @NotNull
    private final WebmentionManagementApiClient webmentionManagementApiClient;
    private final int                           webmentionId;

    public PublishWebmentionCommand(@NotNull String[] args) throws MissingRequiredParameter, InvalidParameterValueException {
        var argsMap = getArgs(args, PublishWebmentionCommand.class);
        this.webmentionManagementApiClient = new WebmentionManagementApiClient(
                getArgOfTypeOrThrow(argsMap, "uri", URI.class),
                getArgOfTypeOrThrow(argsMap, "token-endpoint", String.class),
                getArgOfTypeOrThrow(argsMap, "management-endpoint", String.class)
        );
        this.webmentionId                  = (Integer) argsMap.get("id");
    }

    @Override
    public void run() {
        var webmention = webmentionManagementApiClient.getWebmention(webmentionId);
        if (webmention == null) {
            System.err.println("Webmention with id " + webmentionId + " not found");
        } else {
            webmentionManagementApiClient.publishWebmention(webmentionId);
        }
    }
}
