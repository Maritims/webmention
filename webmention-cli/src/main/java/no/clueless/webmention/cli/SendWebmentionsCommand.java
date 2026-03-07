package no.clueless.webmention.cli;

import no.clueless.webmention.api_client.WebmentionApiClient;
import no.clueless.webmention.event.WebmentionEvent;
import no.clueless.webmention.persistence.Webmention;
import no.clueless.webmention.receiver.WebmentionHtmlSourceScanner;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.Set;

@Command(
        name = "send-webmention",
        description = "Sends a webmention.",
        parameters = {
                @CommandParameter(longName = "uri", shortName = "u", description = "uri", required = true, requiresValue = true, type = URI.class),
                @CommandParameter(longName = "dir", shortName = "d", description = "Path to scan for webmentions.", requiresValue = true, type = Path.class),
                @CommandParameter(longName = "dry-run", shortName = "dr", description = "Dry run.", type = Boolean.class, defaultValue = "true"),
                @CommandParameter(longName = "restrictive", shortName = "r", description = "Restrictive mode.", type = Boolean.class, defaultValue = "false")
        }
)
public class SendWebmentionsCommand extends CommandBase {
    private static final Logger log = LoggerFactory.getLogger(SendWebmentionsCommand.class);

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

    public SendWebmentionsCommand(@NotNull String[] args) {
        var argsMap = getArgs(args, SendWebmentionsCommand.class);
        this.webmentionApiClient       = new WebmentionApiClient((URI) argsMap.get("uri"), HttpClient.newHttpClient());
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
            webmentionEvents = webmentionDirectoryWalker.walk(baseUri, dir, null);
        } catch (IOException e) {
            throw new RuntimeException("Failed to walk directory", e);
        }

        webmentionEvents.forEach(webmentionEvent -> {
            if (dryRun) {
                log.info("[DRY RUN] Would have sent webmention from {} to {}", webmentionEvent.sourceUrl(), webmentionEvent.targetUrl());
            } else {
                webmentionApiClient.postWebmention(Webmention.newWebmention(webmentionEvent.sourceUrl(), webmentionEvent.targetUrl(), webmentionEvent.mentionText()));
            }
        });
    }
}
