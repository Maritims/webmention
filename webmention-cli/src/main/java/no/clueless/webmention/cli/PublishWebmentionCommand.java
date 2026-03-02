package no.clueless.webmention.cli;

import no.clueless.webmention.api_client.WebmentionManagementApiClient;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.List;

public class PublishWebmentionCommand implements Command {
    @NotNull
    private final WebmentionManagementApiClient webmentionManagementApiClient;
    private final int                           webmentionId;

    public PublishWebmentionCommand(@NotNull WebmentionManagementApiClient webmentionManagementApiClient, int webmentionId) {
        this.webmentionManagementApiClient = webmentionManagementApiClient;
        this.webmentionId                  = webmentionId;
    }

    @Override
    public String name() {
        return "publish-webmention";
    }

    @Override
    public void run() {
        webmentionManagementApiClient.publishWebmention(webmentionId);
    }

    public static class Factory implements Command.Factory<PublishWebmentionCommand> {
        @Override
        public @NotNull List<Parameter<?>> parameters() {
            return List.of(new Parameter<>("id", "i", "The webmention id.", true, true, null, Integer::parseInt, null));
        }

        @Override
        public @NotNull PublishWebmentionCommand createCommand(@NotNull String[] args) throws FactoryException {
            var argsMap = getArgs(args);
            return new PublishWebmentionCommand(new WebmentionManagementApiClient((URI) argsMap.get("uri")), (Integer) argsMap.get("id"));
        }

        @Override
        public @NotNull String description() {
            return "Publish a webmention.";
        }
    }
}
