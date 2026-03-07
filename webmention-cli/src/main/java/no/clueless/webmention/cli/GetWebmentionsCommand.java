package no.clueless.webmention.cli;

import no.clueless.webmention.api_client.Pagination;
import no.clueless.webmention.api_client.WebmentionManagementApiClient;
import no.clueless.webmention.persistence.Webmention;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

@Command(
        name = "get-webmentions",
        description = "Gets webmentions on behalf of a given base URI.",
        parameters = {
                @CommandParameter(longName = "uri", shortName = "u", description = "uri", required = true, requiresValue = true, type = URI.class),
                @CommandParameter(longName = "page", shortName = "p", description = "The page number.", requiresValue = true, defaultValue = "0", type = Integer.class),
                @CommandParameter(longName = "size", shortName = "s", description = "The page size.", requiresValue = true, defaultValue = "10", type = Integer.class),
                @CommandParameter(longName = "is-approved", shortName = "a", description = "Whether to only get approved webmentions.", defaultValue = "false", type = Boolean.class)
        }
)
public class GetWebmentionsCommand extends CommandBase {
    private static final Logger log = LoggerFactory.getLogger(GetWebmentionsCommand.class);

    @NotNull
    private final WebmentionManagementApiClient webmentionManagementApiClient;
    private final int                           page;
    private final int                           size;
    private final boolean                       isApproved;

    public GetWebmentionsCommand(@NotNull String[] args) {
        var argsMap = getArgs(args, GetWebmentionsCommand.class);
        this.webmentionManagementApiClient = new WebmentionManagementApiClient(getArgOfTypeOrThrow(argsMap, "uri", URI.class));
        this.page                          = getArgOfTypeOrThrow(argsMap, "page", Integer.class);
        this.size                          = getArgOfTypeOrThrow(argsMap, "size", Integer.class);
        this.isApproved                    = getArgOfTypeOrThrow(argsMap, "is-approved", Boolean.class);
    }

    @Override
    public String name() {
        return "get-webmentions";
    }

    @Override
    public @NotNull String description() {
        return "Get webmentions on behalf of a given base URI.";
    }

    @Override
    public void run() {
        webmentionManagementApiClient.getWebmentions(new Pagination(page, size), isApproved)
                .stream()
                .map(Webmention::toString)
                .forEach(log::info);
    }
}
