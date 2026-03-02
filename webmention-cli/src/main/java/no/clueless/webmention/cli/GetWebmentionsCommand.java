package no.clueless.webmention.cli;

import no.clueless.webmention.api_client.Pagination;
import no.clueless.webmention.api_client.WebmentionManagementApiClient;
import no.clueless.webmention.persistence.Webmention;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;

public class GetWebmentionsCommand implements Command, Runnable {
    private static final Logger log = LoggerFactory.getLogger(GetWebmentionsCommand.class);

    @NotNull
    private final WebmentionManagementApiClient webmentionManagementApiClient;
    private final int                           page;
    private final int                           size;
    private final boolean                       isApproved;

    public GetWebmentionsCommand(@NotNull WebmentionManagementApiClient webmentionManagementApiClient, int page, int size, boolean isApproved) {
        this.webmentionManagementApiClient = webmentionManagementApiClient;
        this.page                          = page;
        this.size                          = size;
        this.isApproved                    = isApproved;
    }

    @Override
    public String name() {
        return "get-webmentions";
    }

    @Override
    public void run() {
        webmentionManagementApiClient.getWebmentions(new Pagination(page, size), isApproved)
                .stream()
                .map(Webmention::toString)
                .forEach(log::info);
    }

    public static class Factory implements Command.Factory<GetWebmentionsCommand> {
        @Override
        public @NotNull List<Parameter<?>> parameters() {
            return List.of(
                    URIParameter.required("uri", "u", "URI to get webmentions for."),
                    new Parameter<>("page", "p", "The page number.", false, true, 0, Integer::parseInt, null),
                    new Parameter<>("size", "s", "The page size.", false, true, 10, Integer::parseInt, null),
                    new Parameter<>("is-approved", "a", "Whether to only get approved webmentions.", false, true, false, Boolean::parseBoolean, null)
            );
        }

        @Override
        public @NotNull GetWebmentionsCommand createCommand(@NotNull String[] args) throws FactoryException {
            var argsMap = getArgs(args);
            return new GetWebmentionsCommand(new WebmentionManagementApiClient((URI) argsMap.get("uri")), (Integer) argsMap.get("page"), (Integer) argsMap.get("size"), (Boolean) argsMap.get("is-approved"));
        }

        @Override
        public @NotNull String description() {
            return "Get webmentions on behalf of a given base URI.";
        }
    }
}
