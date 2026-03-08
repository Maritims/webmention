package no.clueless.webmention.cli;

import no.clueless.webmention.api_client.WebmentionApiClient;
import no.clueless.webmention.core.event.WebmentionEvent;
import no.clueless.webmention.core.persistence.Webmention;
import no.clueless.webmention.core.receiver.WebmentionHtmlSourceScanner;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.Set;

@Command(
        name = "send",
        description = "Sends a webmention.",
        parameters = {
                @CommandParameter(longName = "uri", shortName = "u", description = "The base API URI.", requiresValue = true, required = true, type = URI.class),
                @CommandParameter(longName = "public-endpoint", shortName = "e", description = "The public endpoint URI.", requiresValue = true, defaultValue = "/webmention", type = String.class),
                @CommandParameter(longName = "dir", shortName = "d", description = "Path to scan for webmentions.", requiresValue = true, required = true, type = Path.class),
                @CommandParameter(longName = "dry-run", shortName = "dr", description = "Dry run.", type = Boolean.class, defaultValue = "true"),
                @CommandParameter(longName = "restrictive", shortName = "r", description = "Restrictive mode.", type = Boolean.class, defaultValue = "false")
        }
)
public class SendWebmentionsCommand extends CommandBase {
    @NotNull
    private final WebmentionApiClient       webmentionApiClient;
    @NotNull
    private final WebmentionDirectoryWalker webmentionDirectoryWalker;
    @NotNull
    private final URI                       baseUri;
    @NotNull
    private final Path                      dir;
    private final boolean                   dryRun;
    private final boolean                   restrictive;

    public SendWebmentionsCommand(@NotNull String[] args) throws MissingRequiredParameter, InvalidParameterValueException {
        var argsMap = getArgs(args, SendWebmentionsCommand.class);
        this.webmentionApiClient       = new WebmentionApiClient(
                getArgOfTypeOrThrow(argsMap, "uri", URI.class),
                getArgOfTypeOrThrow(argsMap, "public-endpoint", String.class),
                HttpClient.newHttpClient()
        );
        this.webmentionDirectoryWalker = new WebmentionDirectoryWalker(new WebmentionHtmlSourceScanner(), Set.of("html"));
        this.baseUri                   = (URI) argsMap.get("uri");
        this.dir                       = (Path) argsMap.get("dir");
        this.dryRun                    = (Boolean) argsMap.get("dry-run");
        this.restrictive               = (Boolean) argsMap.get("restrictive");
    }

    @Override
    public String name() {
        return "send-webmentions";
    }

    @Override
    public void run() {
        Set<WebmentionEvent> webmentionEvents;
        try {
            webmentionEvents = webmentionDirectoryWalker.walk(baseUri, dir, restrictive ? (element) -> element.dataset().containsKey("webmention") : null);
        } catch (IOException e) {
            throw new RuntimeException("Failed to walk directory", e);
        }

        if (webmentionEvents.isEmpty()) {
            System.out.println("No webmentions found");
        } else {
            webmentionEvents.forEach(webmentionEvent -> {
                if (dryRun) {
                    System.out.printf("[DRY RUN] Would have sent webmention from %s to %s%n", webmentionEvent.sourceUrl(), webmentionEvent.targetUrl());
                } else {
                    webmentionApiClient.postWebmention(Webmention.newWebmention(webmentionEvent.sourceUrl(), webmentionEvent.targetUrl(), webmentionEvent.mentionText()));
                }
            });
        }
    }
}
