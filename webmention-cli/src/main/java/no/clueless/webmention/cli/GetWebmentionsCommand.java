package no.clueless.webmention.cli;

import no.clueless.webmention.api_client.Pagination;
import no.clueless.webmention.api_client.WebmentionManagementApiClient;
import no.clueless.webmention.core.persistence.Webmention;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.stream.Collectors;

@Command(
        name = "get-webmentions",
        description = "Gets webmentions on behalf of a given base URI.",
        parameters = {
                @CommandParameter(longName = "uri", shortName = "u", description = "The base API URI.", requiresValue = true, required = true, type = URI.class),
                @CommandParameter(longName = "token-endpoint", shortName = "t", description = "The token endpoint URI.", requiresValue = true, defaultValue = "/oauth/token", type = String.class),
                @CommandParameter(longName = "management-endpoint", shortName = "m", description = "The webmention management endpoint URI.", requiresValue = true, defaultValue = "/webmention/manage", type = String.class),
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

    public GetWebmentionsCommand(@NotNull String[] args) throws MissingRequiredParameter, InvalidParameterValueException {
        var argsMap = getArgs(args, GetWebmentionsCommand.class);
        this.webmentionManagementApiClient = new WebmentionManagementApiClient(
                getArgOfTypeOrThrow(argsMap, "uri", URI.class),
                getArgOfTypeOrThrow(argsMap, "token-endpoint", String.class),
                getArgOfTypeOrThrow(argsMap, "management-endpoint", String.class)
        );
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
        if (log.isDebugEnabled()) {
            log.debug("Using arguments: page={}, size={}, isApproved={}", page, size, isApproved);
        }

        var webmentions = webmentionManagementApiClient.getWebmentions(new Pagination(page, size), isApproved)
                .stream()
                .map(Webmention::toString)
                .collect(Collectors.toSet());

        if (webmentions.isEmpty()) {
            System.out.println("No webmentions found");
            return;
        }

        for (var webmention : webmentions) {
            System.out.println(webmention);
        }
    }
}
