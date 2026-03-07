package no.clueless.webmention.cli;

import no.clueless.webmention.api_client.WebmentionManagementApiClient;
import org.jetbrains.annotations.NotNull;

import java.net.URI;

@Command(
        name = "publish-webmention",
        description = "Publishes a webmention by its id.",
        parameters = {
                @CommandParameter(longName = "uri", shortName = "u", description = "The webmention management API URI.", requiresValue = true, required = true, type = URI.class),
                @CommandParameter(longName = "id", shortName = "i", description = "The webmention id.", requiresValue = true, required = true, type = Integer.class)
        }
)
public class PublishWebmentionCommand extends CommandBase {
    @NotNull
    private final WebmentionManagementApiClient webmentionManagementApiClient;
    private final int                           webmentionId;

    public PublishWebmentionCommand(@NotNull String[] args) {
        var argsMap = getArgs(args, PublishWebmentionCommand.class);
        this.webmentionManagementApiClient = new WebmentionManagementApiClient((URI) argsMap.get("uri"));
        this.webmentionId                  = (Integer) argsMap.get("id");
    }

    @Override
    public void run() {
        webmentionManagementApiClient.publishWebmention(webmentionId);
    }
}
