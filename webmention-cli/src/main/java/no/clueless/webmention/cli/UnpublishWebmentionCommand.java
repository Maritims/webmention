package no.clueless.webmention.cli;

import no.clueless.webmention.api_client.WebmentionManagementApiClient;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.List;

public class UnpublishWebmentionCommand implements Command {
    @NotNull
    private final WebmentionManagementApiClient webmentionManagementApiClient;
    private final int                           webmentionId;

    public UnpublishWebmentionCommand(@NotNull WebmentionManagementApiClient webmentionManagementApiClient, int webmentionId) {
        this.webmentionManagementApiClient = webmentionManagementApiClient;
        this.webmentionId                  = webmentionId;
    }

    @Override
    public String name() {
        return "unpublish-webmention";
    }

    @Override
    public void run() {
        webmentionManagementApiClient.unpublishWebmention(webmentionId);
    }

    public static final class Factory implements Command.Factory<UnpublishWebmentionCommand> {
        @Override
        public @NotNull List<Parameter<?>> parameters() {
            return List.of(new Parameter<>("id", "i", "The webmention id.", true, true, null, Integer::parseInt, null));
        }

        @Override
        public @NotNull UnpublishWebmentionCommand createCommand(@NotNull String[] args) throws FactoryException {
            var argsMap = getArgs(args);
            return new UnpublishWebmentionCommand(new WebmentionManagementApiClient((URI) argsMap.get("uri")), (Integer) argsMap.get("id"));
        }

        @Override
        public @NotNull String description() {
            return "Unpublish a webmention.";
        }
    }
}
