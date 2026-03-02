package no.clueless.webmention.cli;

import no.clueless.webmention.api_client.WebmentionManagementApiClient;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.List;

public class DeleteWebmentionCommand implements Command {
    @NotNull
    private final WebmentionManagementApiClient webmentionManagementApiClient;
    private final int                           webmentionId;

    public DeleteWebmentionCommand(@NotNull WebmentionManagementApiClient webmentionManagementApiClient, int webmentionId) {
        this.webmentionManagementApiClient = webmentionManagementApiClient;
        this.webmentionId                  = webmentionId;
    }

    @Override
    public String name() {
        return "delete-webmention";
    }

    @Override
    public void run() {
        webmentionManagementApiClient.deleteWebmention(webmentionId);
    }

    public static final class Factory implements Command.Factory<DeleteWebmentionCommand> {
        @Override
        public @NotNull List<Parameter<?>> parameters() {
            return List.of(new Parameter<>("id", "i", "The webmention id.", true, true, null, Integer::parseInt, null));
        }

        @Override
        public @NotNull DeleteWebmentionCommand createCommand(@NotNull String[] args) throws FactoryException {
            var argsMap = getArgs(args);
            return new DeleteWebmentionCommand(new WebmentionManagementApiClient((URI) argsMap.get("uri")), (Integer) argsMap.get("id"));
        }

        @Override
        public @NotNull String description() {
            return "Delete a webmention.";
        }
    }
}
