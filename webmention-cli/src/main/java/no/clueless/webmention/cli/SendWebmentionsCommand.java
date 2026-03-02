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
import java.util.List;
import java.util.Set;

public class SendWebmentionsCommand implements Command {
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

    public SendWebmentionsCommand(@NotNull WebmentionApiClient webmentionApiClient, @NotNull WebmentionDirectoryWalker webmentionDirectoryWalker, @NotNull URI baseUri, @NotNull Path dir, boolean dryRun, boolean restrictive) {
        this.webmentionApiClient       = webmentionApiClient;
        this.webmentionDirectoryWalker = webmentionDirectoryWalker;
        this.baseUri                   = baseUri;
        this.dir                       = dir;
        this.dryRun                    = dryRun;
        this.restrictive               = restrictive;
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

    public static class Factory implements Command.Factory<SendWebmentionsCommand> {
        @Override
        public @NotNull List<Parameter<?>> parameters() {
            return List.of(
                    URIParameter.required("--uri", "-u", "URI to send webmentions for."),
                    new Parameter<>("--dir", "-d", "Path to scan for webmentions.", true, true, null, Path::of, null),
                    new Parameter<>("--dry-run", "-dr", "Dry run.", false, false, true, ignored -> true, null),
                    new Parameter<>("--restrictive", "-r", "Restrictive mode.", false, false, true, ignored -> true, null)
            );
        }

        @Override
        public @NotNull SendWebmentionsCommand createCommand(@NotNull String[] args) throws FactoryException {
            var argsMap = getArgs(args);
            return new SendWebmentionsCommand(new WebmentionApiClient((URI) argsMap.get("uri"), HttpClient.newHttpClient()), new WebmentionDirectoryWalker(new WebmentionHtmlSourceScanner(), Set.of("html")), (URI) argsMap.get("uri"), (Path) argsMap.get("dir"), (Boolean) argsMap.get("dry-run"), (Boolean) argsMap.get("restrictive"));
        }

        @Override
        public @NotNull String description() {
            return "Sends webmentions on behalf of a given base URI.";
        }
    }
}
